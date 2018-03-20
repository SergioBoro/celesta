package ru.curs.celesta.dbutils.adaptors.column;

import static ru.curs.celesta.dbutils.adaptors.constants.CommonConstants.*;
import static ru.curs.celesta.dbutils.adaptors.constants.OpenSourceConstants.*;

import ru.curs.celesta.score.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;



class H2IntegerColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "integer";
    }

    @Override
    public String getMainDefinition(Column c) {
        return join(c.getQuotedName(), dbFieldType(), nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        IntegerColumn ic = (IntegerColumn) c;
        String defaultStr = "";

        SequenceElement s = ic.getSequence();
        if (s != null) {
            defaultStr = DEFAULT + s.getGrain().getQuotedName() + "." + s.getQuotedName() + ".nextval";
        } else if (ic.getDefaultValue() != null) {
            defaultStr = DEFAULT + ic.getDefaultValue();
        }
        return defaultStr;
    }
}

class H2FloatingColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "double";
    }

    @Override
    public String getMainDefinition(Column c) {
        return join(c.getQuotedName(), dbFieldType(), nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        FloatingColumn ic = (FloatingColumn) c;
        String defaultStr = "";
        if (ic.getDefaultValue() != null) {
            defaultStr = DEFAULT + ic.getDefaultValue();
        }
        return defaultStr;
    }
}

class H2BooleanColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "boolean";
    }

    @Override
    public String getMainDefinition(Column c) {
        return join(c.getQuotedName(), dbFieldType(), nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        BooleanColumn ic = (BooleanColumn) c;
        String defaultStr = "";
        if (ic.getDefaultValue() != null) {
            defaultStr = DEFAULT + ic.getDefaultValue();
        }
        return defaultStr;
    }
}

class H2StringColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "varchar";
    }

    @Override
    public String getMainDefinition(Column c) {
        StringColumn ic = (StringColumn) c;
        String fieldType = ic.isMax() ? "clob" : String.format("%s(%s)", dbFieldType(), ic.getLength());
        return join(c.getQuotedName(), fieldType, nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        StringColumn ic = (StringColumn) c;
        String defaultStr = "";
        if (ic.getDefaultValue() != null) {
            defaultStr = DEFAULT + StringColumn.quoteString(ic.getDefaultValue());
        }
        return defaultStr;
    }
}

class H2BinaryColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "varbinary";
    }

    @Override
    public String getMainDefinition(Column c) {
        return join(c.getQuotedName(), dbFieldType(), nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        BinaryColumn bc = (BinaryColumn) c;
        String defaultStr = "";
        if (bc.getDefaultValue() != null) {
            Matcher m = HEXSTR.matcher(bc.getDefaultValue());
            m.matches();
            defaultStr = DEFAULT + String.format("X'%s'", m.group(1));
        }
        return defaultStr;
    }
}

class H2DateTimeColumnDefiner extends ColumnDefiner {
    @Override
    public String dbFieldType() {
        return "timestamp";
    }

    @Override
    public String getMainDefinition(Column c) {
        return join(c.getQuotedName(), dbFieldType(), nullable(c));
    }

    @Override
    public String getDefaultDefinition(Column c) {
        DateTimeColumn ic = (DateTimeColumn) c;
        String defaultStr = "";
        if (ic.isGetdate()) {
            defaultStr = DEFAULT + NOW;
        } else if (ic.getDefaultValue() != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            defaultStr = String.format(DEFAULT + " '%s'", df.format(ic.getDefaultValue()));

        }
        return defaultStr;
    }
}