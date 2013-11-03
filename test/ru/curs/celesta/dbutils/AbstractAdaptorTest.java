package ru.curs.celesta.dbutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;
import ru.curs.celesta.dbutils.DBAdaptor.IndexInfo;
import ru.curs.celesta.score.Column;
import ru.curs.celesta.score.Grain;
import ru.curs.celesta.score.Index;
import ru.curs.celesta.score.IntegerColumn;
import ru.curs.celesta.score.ParseException;
import ru.curs.celesta.score.Score;
import ru.curs.celesta.score.Table;

public abstract class AbstractAdaptorTest {
	final static String GRAIN_NAME = "gtest";
	final static String SCORE_NAME = "testScore";
	private DBAdaptor dba;
	private Score score;

	@Before
	public void setup() throws Exception {
		Connection conn = ConnectionPool.get();
		try {
			dba.createSchemaIfNotExists(conn, GRAIN_NAME);
		} finally {
			ConnectionPool.putBack(conn);
		}
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		try {
			dba.dropTable(t);
		} catch (Exception e) {
		}
	}

	@Test
	public void tableExists() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		try {
			boolean result = dba.tableExists(t.getGrain().getName(),
					t.getName());
			assertTrue(result);
		} finally {
			dba.dropTable(t);
		}
	}

	@Test
	public void userTablesExist() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		try {
			boolean result = dba.userTablesExist();
			assertTrue(result);
		} finally {
			dba.dropTable(t);
		}
	}

	@Test
	public void createTable() throws CelestaException {
		try {
			Table t = score.getGrain(GRAIN_NAME).getTable("test");
			dba.createTable(t);
			dba.dropTable(t);
		} catch (Exception ex) {
			fail("Threw Exception:" + ex.getMessage());
		}
	}

	private int insertRow(Connection conn, Table t, int val) throws Exception {
		int count = t.getColumns().size();
		assertEquals(13, count);
		boolean[] nullsMask = { true, false, false, false, true, false, true,
				true, false, true, true, true, false };
		BLOB b = new BLOB();
		b.getOutStream().write(new byte[] { 1, 2, 3 });
		Object[] rowData = { "ab", val, false, 1.1, "eee", b };
		PreparedStatement pstmt = dba.getInsertRecordStatement(conn, t,
				nullsMask);
		assertNotNull(pstmt);
		int i = 1;
		for (Object fieldVal : rowData)
			DBAdaptor.setParam(pstmt, i++, fieldVal);
		try {
			int rowCount = pstmt.executeUpdate();
			return rowCount;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void getInsertRecordStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			int rowCount = insertRow(conn, t, 1);
			assertTrue(rowCount == 1);
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getSetCountStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			PreparedStatement stmt = dba.getSetCountStatement(conn, t,
					Collections.<String, AbstractFilter> emptyMap());
			ResultSet rs = stmt.executeQuery();
			rs.next();
			assertEquals(0, rs.getInt(1));

			insertRow(conn, t, 1);
			insertRow(conn, t, 2);
			insertRow(conn, t, 3);

			rs = stmt.executeQuery();
			rs.next();
			assertEquals(3, rs.getInt(1));

		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getUpdateRecordStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			insertRow(conn, t, 1);
			int count = t.getColumns().size();
			assertEquals(13, count);
			boolean[] mask = { true, true, false, true, true, true, true, true,
					true, true, true, true, true };

			PreparedStatement pstmt = dba.getUpdateRecordStatement(conn, t,
					mask);
			assertNotNull(pstmt);
			DBAdaptor.setParam(pstmt, 1, 2); // field value
			DBAdaptor.setParam(pstmt, 2, 1); // key value
			int rowCount = pstmt.executeUpdate();
			assertTrue(rowCount == 1);
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getDeleteRecordStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			insertRow(conn, t, 1);
			PreparedStatement pstmt = dba.getDeleteRecordStatement(conn, t);
			assertNotNull(pstmt);
			DBAdaptor.setParam(pstmt, 1, 1);// key value
			int rowCount = pstmt.executeUpdate();
			assertTrue(rowCount == 1);
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getColumns() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			Set<String> columnSet = dba.getColumns(conn, t);
			assertNotNull(columnSet);
			assertEquals(13, columnSet.size());
			assertTrue(columnSet.contains("f4"));
			assertFalse(columnSet.contains("nonExistentColumn"));
			// String[] columnNames = { "id", "attrVarchar", "attrInt", "f1",
			// "f2", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11" };
			// System.out
			// .println(Arrays.toString(columnSet.toArray(new String[0])));

		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getIndices() throws Exception {
		Grain grain = score.getGrain(GRAIN_NAME);
		Table t = grain.getTable("test");
		dba.createTable(t);
		Index i = grain.getIndices().get("idxtest");
		dba.createIndex(i);
		Connection conn = ConnectionPool.get();
		try {
			Map<IndexInfo, TreeMap<Short, String>> indicesSet = dba.getIndices(
					conn, t.getGrain());
			assertNotNull(indicesSet);
			assertTrue(indicesSet.size() == 1);
			dba.dropIndex(grain,
					new DBAdaptor.IndexInfo(t.getName(), i.getName()));

			indicesSet = dba.getIndices(conn, t.getGrain());
			assertNotNull(indicesSet);
			assertTrue(indicesSet.size() == 0);
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getOneFieldStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			insertRow(conn, t, 1);
			Column c = t.getColumns().get("attrInt");
			PreparedStatement pstmt = dba.getOneFieldStatement(conn, c);
			assertNotNull(pstmt);
			DBAdaptor.setParam(pstmt, 1, 1);// key value
			ResultSet rs = pstmt.executeQuery();
			assertTrue(rs.next());
			rs.getInt("attrInt");
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getOneRecordStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			insertRow(conn, t, 1);
			PreparedStatement pstmt = dba.getOneRecordStatement(conn, t);
			assertNotNull(pstmt);
			DBAdaptor.setParam(pstmt, 1, 1);// key value
			ResultSet rs = pstmt.executeQuery();
			assertTrue(rs.next());
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void deleteRecordSetStatement() throws Exception {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		Connection conn = ConnectionPool.get();
		try {
			insertRow(conn, t, 1);
			PreparedStatement pstmt = dba.deleteRecordSetStatement(conn, t,
					Collections.<String, AbstractFilter> emptyMap());
			assertNotNull(pstmt);
			int rowCount = pstmt.executeUpdate();
			assertTrue(rowCount == 1);
		} finally {
			ConnectionPool.putBack(conn);
			dba.dropTable(t);
		}
	}

	@Test
	public void getColumnInfo1() throws CelestaException, ParseException {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		DBAdaptor.ColumnInfo c;

		// Проверяем реакцию на столбец, которого нет в базе данных
		Column newCol = new IntegerColumn(t, "nonExistentColumn");
		assertSame(newCol, t.getColumn("nonExistentColumn"));
		c = dba.getColumnInfo(newCol);
		assertNull(c);

		// Этот тест проверяет типы колонок и выражения not null
		// attrVarchar nvarchar(2),
		c = dba.getColumnInfo(t.getColumn("attrVarchar"));
		assertEquals("attrVarchar", c.getName());
		assertEquals(DBAdaptor.DBColumnType.NVARCHAR, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(2, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f1 bit not null,
		c = dba.getColumnInfo(t.getColumn("f1"));
		assertEquals("f1", c.getName());
		assertEquals(DBAdaptor.DBColumnType.BIT, c.getType());
		assertEquals(false, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f4 real,
		c = dba.getColumnInfo(t.getColumn("f4"));
		assertEquals("f4", c.getName());
		assertEquals(DBAdaptor.DBColumnType.REAL, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f7 nvarchar(8),
		c = dba.getColumnInfo(t.getColumn("f7"));
		assertEquals("f7", c.getName());
		assertEquals(DBAdaptor.DBColumnType.NVARCHAR, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(8, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f11 image not null
		c = dba.getColumnInfo(t.getColumn("f11"));
		assertEquals("f11", c.getName());
		assertEquals(DBAdaptor.DBColumnType.IMAGE, c.getType());
		assertEquals(false, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());
	}

	@Test
	public void getColumnInfo2() throws CelestaException, ParseException {
		Table t = score.getGrain(GRAIN_NAME).getTable("test");
		dba.createTable(t);
		DBAdaptor.ColumnInfo c;
		// Этот тест проверяет выражения default и дополнительные атрибуты
		// id int identity not null primary key,
		c = dba.getColumnInfo(t.getColumn("id"));
		assertEquals("id", c.getName());
		assertEquals(DBAdaptor.DBColumnType.INT, c.getType());
		assertEquals(false, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(true, c.isIdentity());

		// attrInt int default 3,
		c = dba.getColumnInfo(t.getColumn("attrInt"));
		assertEquals("attrInt", c.getName());
		assertEquals(DBAdaptor.DBColumnType.INT, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("3", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f2 bit default 'true',
		c = dba.getColumnInfo(t.getColumn("f2"));
		assertEquals("f2", c.getName());
		assertEquals(DBAdaptor.DBColumnType.BIT, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("'TRUE'", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f5 real not null default 5.5,
		c = dba.getColumnInfo(t.getColumn("f5"));
		assertEquals("f5", c.getName());
		assertEquals(DBAdaptor.DBColumnType.REAL, c.getType());
		assertEquals(false, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("5.5", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f6 nvarchar(max) not null default 'abc',
		c = dba.getColumnInfo(t.getColumn("f6"));
		assertEquals("f6", c.getName());
		assertEquals(DBAdaptor.DBColumnType.NVARCHAR, c.getType());
		assertEquals(false, c.isNullable());
		// assertEquals(0, c.getLength()); -- database-dependent value
		assertEquals("'abc'", c.getDefaultValue());
		assertEquals(true, c.isMax());
		assertEquals(false, c.isIdentity());

		// f8 datetime default '20130401',
		c = dba.getColumnInfo(t.getColumn("f8"));
		assertEquals("f8", c.getName());
		assertEquals(DBAdaptor.DBColumnType.DATETIME, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("'20130401'", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f9 datetime not null default getdate(),
		c = dba.getColumnInfo(t.getColumn("f9"));
		assertEquals("f9", c.getName());
		assertEquals(DBAdaptor.DBColumnType.DATETIME, c.getType());
		assertEquals(false, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("GETDATE()", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());

		// f10 image default 0xFFAAFFAAFF,
		c = dba.getColumnInfo(t.getColumn("f10"));
		assertEquals("f10", c.getName());
		assertEquals(DBAdaptor.DBColumnType.IMAGE, c.getType());
		assertEquals(true, c.isNullable());
		assertEquals(0, c.getLength());
		assertEquals("0xFFAAFFAAFF", c.getDefaultValue());
		assertEquals(false, c.isMax());
		assertEquals(false, c.isIdentity());
	}

	protected void setDba(DBAdaptor dba) {
		this.dba = dba;
	}

	protected void setScore(Score score) {
		this.score = score;
	}
}
