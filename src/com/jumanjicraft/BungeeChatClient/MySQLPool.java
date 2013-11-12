package com.jumanjicraft.BungeeChatClient;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class MySQLPool implements AutoCloseable {
	
	private final static int connectionNum = 3;
	private final static long timeoutTime = 300000L;
	private final List<MyConnection> connections;
	private final String url;
	private final String user;
	private final String pass;
	//private final Lock lock = new ReentrantLock();
	
	public MySQLPool(String url, String user, String pass) throws ClassNotFoundException
	{
		Class.forName("com.mysql.jdbc.Driver");
		this.url = url;
		this.user = user;
		this.pass = pass;
		this.connections = Collections.synchronizedList(new ArrayList<MyConnection>(connectionNum));
		MyConnectionReaper reaper = new MyConnectionReaper(timeoutTime);
		new Thread(reaper, "MySQL connection reaper thread - JChat").start();
	}
	
	public void close()
	{
		synchronized(connections)
		{
			Iterator<MyConnection> itr = connections.iterator();
			while (itr.hasNext())
			{
				itr.next().terminate();
				itr.remove();
			}
		}
	}
	
	public Connection getConnection() throws SQLException {
		synchronized (connections)
		{
			Iterator<MyConnection> itr = connections.iterator();
			while (itr.hasNext())
			{
				MyConnection conn = itr.next();
				if (conn.lease())
				{
					if (conn.isValid())
					{
						return conn;
					} else {
						conn.terminate();
						itr.remove();
					}
				}
			}
			final MyConnection conn = new MyConnection(DriverManager.getConnection(url, user, pass));
			conn.lease();
			if (!conn.isValid()) {
				conn.terminate();
				throw new SQLException("Failed to validate a brand new connection");
			}
			connections.add(conn);
			return conn;
		}
	}
	
	private class MyConnection implements Connection {
		
		private final Connection conn;
		private boolean using;
		private int netTimeout;
		private String schema;
		
		public MyConnection(Connection conn)
		{
			this.conn = conn;
			this.using = false;
			this.netTimeout = 30;
			this.schema = "default";
		}
		
		public void clearWarnings() throws SQLException
		{
			this.conn.clearWarnings();
		}
		
		public void close()
		{
			this.using = false;
			try
			{
				if (this.conn.getAutoCommit() == false)
				{
					this.conn.setAutoCommit(true);
				}
			} catch (SQLException ex) {
				connections.remove(this);
				
			}
		}
		
		public void commit() throws SQLException
		{
			this.conn.commit();
		}
		
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException
	    {
	      return this.conn.createArrayOf(typeName, elements);
	    }

	    public Blob createBlob() throws SQLException
	    {
	      return this.conn.createBlob();
	    }

	    public Clob createClob() throws SQLException
	    {
	      return this.conn.createClob();
	    }

	    public NClob createNClob() throws SQLException
	    {
	      return this.conn.createNClob();
	    }

	    public SQLXML createSQLXML() throws SQLException
	    {
	      return this.conn.createSQLXML();
	    }

	    public Statement createStatement() throws SQLException
	    {
	      return this.conn.createStatement();
	    }

	    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
	    {
	      return this.conn.createStatement(resultSetType, resultSetConcurrency);
	    }

	    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	    {
	      return this.conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	    }

	    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
	    {
	      return this.conn.createStruct(typeName, attributes);
	    }

	    public boolean getAutoCommit() throws SQLException
	    {
	      return this.conn.getAutoCommit();
	    }

	    public String getCatalog() throws SQLException
	    {
	      return this.conn.getCatalog();
	    }

	    public Properties getClientInfo() throws SQLException
	    {
	      return this.conn.getClientInfo();
	    }

	    public String getClientInfo(String name) throws SQLException
	    {
	      return this.conn.getClientInfo(name);
	    }

	    public int getHoldability() throws SQLException
	    {
	      return this.conn.getHoldability();
	    }

	    public DatabaseMetaData getMetaData() throws SQLException
	    {
	      return this.conn.getMetaData();
	    }

	    public int getTransactionIsolation() throws SQLException
	    {
	      return this.conn.getTransactionIsolation();
	    }

	    public Map<String, Class<?>> getTypeMap() throws SQLException
	    {
	      return this.conn.getTypeMap();
	    }

	    public SQLWarning getWarnings() throws SQLException
	    {
	      return this.conn.getWarnings();
	    }

	    public boolean isClosed() throws SQLException
	    {
	      return this.conn.isClosed();
	    }

	    public boolean isReadOnly() throws SQLException
	    {
	      return this.conn.isReadOnly();
	    }

	    public boolean isValid(int timeout) throws SQLException
	    {
	      return this.conn.isValid(timeout);
	    }

	    public boolean isWrapperFor(Class<?> iface) throws SQLException
	    {
	      return this.conn.isWrapperFor(iface);
	    }

	    public String nativeSQL(String sql) throws SQLException
	    {
	      return this.conn.nativeSQL(sql);
	    }

	    public CallableStatement prepareCall(String sql) throws SQLException
	    {
	      return this.conn.prepareCall(sql);
	    }

	    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	    {
	      return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
	    }

	    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	    {
	      return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	    }

	    public PreparedStatement prepareStatement(String sql) throws SQLException
	    {
	      return this.conn.prepareStatement(sql);
	    }

	    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	    {
	      return this.conn.prepareStatement(sql, autoGeneratedKeys);
	    }

	    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	    {
	      return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
	    }

	    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	    {
	      return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	    }

	    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	    {
	      return this.conn.prepareStatement(sql, columnIndexes);
	    }

	    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	    {
	      return this.conn.prepareStatement(sql, columnNames);
	    }

	    public void releaseSavepoint(Savepoint savepoint) throws SQLException
	    {
	      this.conn.releaseSavepoint(savepoint);
	    }

	    public void rollback() throws SQLException
	    {
	      this.conn.rollback();
	    }

	    public void rollback(Savepoint savepoint) throws SQLException
	    {
	      this.conn.rollback(savepoint);
	    }

	    public void setAutoCommit(boolean autoCommit) throws SQLException
	    {
	      this.conn.setAutoCommit(autoCommit);
	    }

	    public void setCatalog(String catalog) throws SQLException
	    {
	      this.conn.setCatalog(catalog);
	    }

	    public void setClientInfo(Properties properties) throws SQLClientInfoException
	    {
	      this.conn.setClientInfo(properties);
	    }

	    public void setClientInfo(String name, String value) throws SQLClientInfoException
	    {
	      this.conn.setClientInfo(name, value);
	    }

	    public void setHoldability(int holdability) throws SQLException
	    {
	      this.conn.setHoldability(holdability);
	    }

	    public void setReadOnly(boolean readOnly) throws SQLException
	    {
	      this.conn.setReadOnly(readOnly);
	    }

	    public Savepoint setSavepoint() throws SQLException
	    {
	      return this.conn.setSavepoint();
	    }

	    public Savepoint setSavepoint(String name) throws SQLException
	    {
	      return this.conn.setSavepoint(name);
	    }

	    public void setTransactionIsolation(int level) throws SQLException
	    {
	      this.conn.setTransactionIsolation(level);
	    }

	    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
	    {
	      this.conn.setTypeMap(map);
	    }

	    public <T> T unwrap(Class<T> iface) throws SQLException
	    {
	      return this.conn.unwrap(iface);
	    }

	    public int getNetworkTimeout() throws SQLException
	    {
	      return this.netTimeout;
	    }
	    
	    public void setNetworkTimeout(Executor exec, int timeout) throws SQLException
	    {
	    	this.netTimeout = timeout;
	    }
	    
	    public void abort(Executor exec) throws SQLException
	    {
	    	
	    }
	    
	    public String getSchema() throws SQLException
	    {
	      return this.schema;
	    }

	    public void setSchema(String str) throws SQLException
	    {
	      this.schema = str;
	    }

	    boolean isValid() {
	      try {
	        return this.conn.isValid(1); } catch (SQLException ex) {
	      }
	      return false;
	    }

	    synchronized boolean lease()
	    {
	      if (this.using)
	      {
	    	  return false;
	      }
	      this.using = true;
	      return true;
	    }

	    void terminate()
	    {
	    try {
	        this.conn.close();
	      }
	      catch (SQLException ex)
	      {
	      }
	    }

	}

	private class MyConnectionReaper implements Runnable
	{
		private final long timeoutTime;
		
		public MyConnectionReaper(long timeoutTime)
		{
			this.timeoutTime = timeoutTime;
		}
		
		public void run()
		{
			while (true)
			{
				try
				{
					Thread.sleep(timeoutTime);
				} catch (InterruptedException e) {
					
				}
			}
		}
		
	}

}

