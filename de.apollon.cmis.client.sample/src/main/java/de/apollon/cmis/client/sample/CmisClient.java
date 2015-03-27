package de.apollon.cmis.client.sample;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.chemistry.opencmis.client.SessionFactoryFinder;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;

/**
 * @author SHomeier
 */
public class CmisClient
{
	private static Properties props = new Properties();

	// cmis supports paging which is perfekt for handling thousands of objects
	private static int DEFAULT_MAX_ITEMS_PER_PAGE = 25;

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException
	{
		CmisClient cmisClient = new CmisClient();

		// connect to cmis server and iterate over all repositories
		Map<String, String> connectionParams = cmisClient.createDefaultConnectionParams();
		List<Repository> repositories = SessionFactoryFinder.find().getRepositories(connectionParams);
		for (Repository repository : repositories)
		{
			System.out.println("Found repository: " + repository.getName());
		}

		// get the op ctx
		Repository repository = repositories.get(0);

		// you can ask about the capabilities of the repository by:
		RepositoryCapabilities capabilities = repository.getCapabilities();
		System.out.println("\nRepository supports folder tree: " + capabilities.isGetFolderTreeSupported());
		System.out.println("Repository supports descendants: " + capabilities.isGetDescendantsSupported());

		Session session = repository.createSession();
		OperationContext opCtx = cmisClient.getOpCtx(session);

		// ... list root folder of first repository
		Folder rootFolder = session.getRootFolder(opCtx);
		ItemIterable<CmisObject> children = rootFolder.getChildren();
		System.out.println();
		for (CmisObject cmisObject : children)
		{
			if (cmisObject instanceof Document)
			{
				System.out.println("Found Document: " + cmisObject.getName());
			}
			if (cmisObject instanceof Folder)
			{
				System.out.println("Found Folder: " + cmisObject.getName());
				// ... if you want to dive into a subfolder you can use Folder.getChildren()
			}
		}

		// we want to do sth. with a cmis document so we try to find one ...
		Document cmisDocument = cmisClient.getFirstCmisDocument(rootFolder, opCtx);

		// ... we then can ask for the metadata/properties of this object ...
		List<Property<?>> properties = cmisDocument.getProperties();
		System.out.println("\nThe CMIS Document '" + cmisDocument.getName() + "' has the following metadata: ");
		for (Property<?> property : properties)
		{
			System.out.println(property.getId() + " -> " + property.getFirstValue());
		}
		System.out.println();

		// ... now we want to save the content stream to local hard disk
		cmisClient.writeContenStreamToFile(cmisDocument.getContentStream());

		// ... now we get all renditions/previews for this document and write them to hard disk
		List<Rendition> renditions = cmisDocument.getRenditions();
		System.out.println("\nFound " + renditions.size() + " renditions/previews for document: " + cmisDocument.getName());
		for (Rendition rendition : renditions)
		{
			System.out.println("\nFound rendition of kind: " + rendition.getKind());
			cmisClient.writeContenStreamToFile(rendition.getContentStream());
		}

		// if you know the path of an object you can also explicitly get it by its path
		// cmis supports multifiling (same file in multiple folders like symlinks) so there could be multiple paths
		// we simply take the first one here
		String path = cmisDocument.getPaths().get(0);
		CmisObject objectByPath = session.getObjectByPath(path);

		// if you know the ID you can get it by id
		String id = cmisDocument.getId();
		CmisObject objectById = session.getObject(id);
	}

	public CmisClient()
	{
		readProperties();
	}

	public Document getFirstCmisDocument(Folder folder, OperationContext opCtx)
	{
		ItemIterable<CmisObject> children = folder.getChildren(opCtx);
		for (CmisObject cmisObject : children)
		{
			if (cmisObject instanceof Document)
			{
				return (Document) cmisObject;
			}
			else if (cmisObject instanceof Folder)
			{
				Folder fldr = (Folder) cmisObject;
				CmisObject co = getFirstCmisDocument(fldr, opCtx);
				if (co != null)
				{
					return (Document) co;
				}
			}
		}
		return null;
	}

	public Map<String, String> createDefaultConnectionParams()
	{
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(SessionParameter.USER, props.getProperty("user"));
		parameters.put(SessionParameter.PASSWORD, props.getProperty("password"));
		parameters.put(SessionParameter.ATOMPUB_URL, props.getProperty("url"));
		parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		// if you want to explicit connect to a known repository then use SessionParameter.REPOSITORY_ID
		// parameters.put(SessionParameter.REPOSITORY_ID, "repository_id_here");

		return parameters;
	}

	public OperationContext getOpCtx(Session session)
	{
		// create new session and operation context
		OperationContext opContext = session.createOperationContext();
		opContext.setMaxItemsPerPage(DEFAULT_MAX_ITEMS_PER_PAGE);
		opContext.setIncludeAcls(false);
		opContext.setIncludeAllowableActions(false);
		opContext.setIncludePolicies(false);
		opContext.setIncludeRelationships(IncludeRelationships.NONE);
		opContext.setRenditionFilterString("cmis:none");
		opContext.setIncludePathSegments(false);
		opContext.setOrderBy("cmis:name");
		opContext.setCacheEnabled(false);
		return opContext;
	}

	public void writeContenStreamToFile(ContentStream cs)
	{
		InputStream in = cs.getStream();
		String targetPath = this.props.getProperty("temp_path") + File.separator + cs.getFileName();
		File file = new File(targetPath);
		System.out.println("Writing stream to file: " + file.getAbsolutePath());
		try
		{
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while (( len = in.read(buf) ) > 0)
			{
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void readProperties()
	{
		InputStream input = null;
		try
		{
			input = getClass().getClassLoader().getResourceAsStream("config.properties");
			// load a properties file
			props.load(input);
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		finally
		{
			if (input != null)
			{
				try
				{
					input.close();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
	}
}
