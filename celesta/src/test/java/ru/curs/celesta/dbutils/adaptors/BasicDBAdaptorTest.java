package ru.curs.celesta.dbutils.adaptors;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import ru.curs.celesta.CelestaException;
import ru.curs.celesta.score.ParseException;
import ru.curs.celesta.score.Score;
import ru.curs.celesta.score.Table;

public class BasicDBAdaptorTest {

	private void testCelestaScore(Score s, DBAdaptor dba, String fileName)
			throws ParseException, IOException {
		String[] actual = (dba.tableDef(s.getGrain("celesta").getElement("grains", Table.class))
				+ "\n" + dba.tableDef(s.getGrain("celesta").getElement("tables", Table.class)))
				.split("\n");
		// for (String l : actual)
		// System.out.println(l);
		BufferedReader r = new BufferedReader(new InputStreamReader(
				BasicDBAdaptorTest.class.getResourceAsStream(fileName), "utf-8"));
		for (String l : actual)
			assertEquals(r.readLine(), l);
	}

	@Test
	public void test1() throws CelestaException, ParseException, IOException {
		Score s = new Score("score");

		//DBAdaptor a = new MSSQLAdaptor();
		//testCelestaScore(s, a, "mssql.txt");

		DBAdaptor a = new PostgresAdaptor(null);
		testCelestaScore(s, a, "postgre.txt");

		//a = new OraAdaptor();
		//testCelestaScore(s, a, "ora.txt");
	}
}