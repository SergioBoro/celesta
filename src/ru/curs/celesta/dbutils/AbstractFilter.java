package ru.curs.celesta.dbutils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.curs.celesta.CelestaException;
import ru.curs.celesta.score.Column;

/**
 * Внутреннее представление фильтра на поле.
 */
abstract class AbstractFilter {

}

/**
 * Фильтр в виде единичного значения.
 */
class SingleValue extends AbstractFilter {
	private final Object value;

	public SingleValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("%s", value);
	}

	public Object getValue() {
		return value;
	}
}

/**
 * Фильтр в виде диапазона значений от..до.
 * 
 */
class Range extends AbstractFilter {
	private final Object valueFrom;
	private final Object valueTo;

	public Range(Object valueFrom, Object valueTo) {
		this.valueFrom = valueFrom;
		this.valueTo = valueTo;
	}

	@Override
	public String toString() {
		return String
				.format("%s..%s", valueFrom.toString(), valueTo.toString());
	}

	public Object getValueFrom() {
		return valueFrom;
	}

	public Object getValueTo() {
		return valueTo;
	}
}

/**
 * Сложный фильтр.
 */
class Filter extends AbstractFilter {
	private static final Pattern LIKETERM = Pattern
			.compile("(\\*?)(([^*]|\\*+[^*])+)(\\**)");

	private final String value;

	public Filter(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("%s", value);
	}

	public String makeWhereClause(Column c) throws CelestaException {
		if ("null".equalsIgnoreCase(value))
			return String.format("%s is null", c.getQuotedName());
		else if ("!null".equalsIgnoreCase(value))
			return String.format("not (%s is null)", c.getQuotedName());

		Matcher m = LIKETERM.matcher(value);
		m.matches();
		return String.format("%s like '%s%s%s'", c.getQuotedName(), m.group(1)
				.isEmpty() ? "" : "%", m.group(2), m.group(4).isEmpty() ? ""
				: "%");
	}
}