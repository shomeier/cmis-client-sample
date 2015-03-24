package de.apollon.cmis.client.sample;

import java.io.IOException;
import java.io.InputStream;
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
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;

/**
 * @author SHomeier
 */
public class CmisClient
{

	private static Properties props = new Properties();

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
		Session session = repository.createSession();
		OperationContext opCtx = cmisClient.getOpCtx(session);

		// ... list root folder of first repository
		Folder rootFolder = session.getRootFolder(opCtx);
		ItemIterable<CmisObject> children = rootFolder.getChildren();
		for (CmisObject cmisObject : children)
		{
			if (cmisObject instanceof Document)
			{
				System.out.println("Found Document: " + cmisObject.getName());
			}
			if (cmisObject instanceof Folder)
			{
				System.out.println("Found Folder: " + cmisObject.getName());
			}
		}

		// ... if you want to dive into a subfolder you can use Folder.getChildren

	}

	public CmisClient()
	{
		readProperties();
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

	private void readProperties()
	{
		InputStream input = null;

		try
		{

			input = getClass().getClassLoader().getResourceAsStream("config.properties");
			// load a properties file
			props.load(input);

		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if (input != null)
			{
				try
				{
					input.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
