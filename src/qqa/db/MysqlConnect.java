package qqa.db;

import java.sql.*;
import weka.core.Instances;
import weka.experiment.InstanceQuery;

public class MysqlConnect{
	public Connection conn = null;
	// TODO: put parameters in configuration file
	private String dbName = "qqa";
	private String serverName = "localhost";
	private String url = "jdbc:mysql://" + serverName +  "/" + dbName;	
	private String driver = "com.mysql.jdbc.Driver";
	private String userName = "mattia"; 
	private String password = "qqa";
	
	/**
	 * Constructing an object connects to the database
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public MysqlConnect() throws InstantiationException, 
	IllegalAccessException, ClassNotFoundException, SQLException{
		Class.forName(driver).newInstance();
		System.out.println("Connecting to the database...");
		conn = DriverManager.getConnection(url,userName,password);
		System.out.println("Successfully connected");	
	}
	
	/**
	 * Disconnects from the database
	 * @throws SQLException
	 */
	public void disconnect() throws SQLException{
		System.out.println("Disconnecting from database...");
		conn.close();
		System.out.println("Successfully disconnected...");
	}
	
	/**
	 * Executes an update: changes database content and returns null
	 * @param query
	 * @throws SQLException
	 */
	public void execute(String query) throws SQLException {
		Statement st = conn.createStatement();
		st.executeUpdate(query);
	}
	
	/**
	 * Asks a query: does not change database and returns a result set
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public ResultSet ask(String query) throws SQLException{
		Statement st = conn.createStatement();
		return st.executeQuery(query);
	}
	
	/**
	 * Special query that constructs a Weka dataset from database content
	 * @param queryString
	 * @return
	 * @throws Exception
	 */
	public Instances retrieveWEKAInstances(String queryStr) throws Exception{
		InstanceQuery query = new InstanceQuery();
		query.setUsername(userName);
		query.setPassword(password);
		query.setQuery(queryStr);
		return query.retrieveInstances();
	}
}