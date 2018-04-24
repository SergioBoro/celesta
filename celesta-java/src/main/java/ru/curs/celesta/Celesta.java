package ru.curs.celesta;

import ru.curs.celesta.score.discovery.DefaultScoreDiscovery;
import ru.curs.celesta.score.discovery.ScoreDiscovery;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Celesta extends AbstractCelesta<JSessionContext> implements ICelesta {

    public static final JSessionContext SYSTEM_SESSION = new JSessionContext(
            SessionContext.SYSTEM_USER_ID, SessionContext.SYSTEM_SESSION_ID
    );

    private final ScoreDiscovery scoreDiscovery = new DefaultScoreDiscovery();

    private Celesta(AppSettings appSettings) {
        super(appSettings, 2);
    }


    public static Celesta createInstance(Properties properties) {
        AppSettings appSettings = preInit(properties);
        return new Celesta(appSettings);
    }

    public static Celesta createInstance() {
        Properties properties = loadPropertiesDynamically();
        return createInstance(properties);
    }

    private static AppSettings preInit(Properties properties) {
        System.out.print("Celesta pre-initialization: phase 1/2 system settings reading...");
        AppSettings appSettings = new AppSettings(properties);
        System.out.println("done.");
        return appSettings;
    }

    private static Properties loadPropertiesDynamically() {
        // Разбираемся с настроечным файлом: читаем его и превращаем в
        // Properties.
        Properties properties = new Properties();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream in = loader.getResourceAsStream(FILE_PROPERTIES);
            if (in == null) {
                throw new CelestaException(
                        String.format("Couldn't find file %s on classpath.", FILE_PROPERTIES)
                );
            }
            try {
                properties.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new CelestaException(
                    String.format("IOException while reading %s file: %s", FILE_PROPERTIES, e.getMessage())
            );
        }

        return properties;
    }


    @Override
    ScoreDiscovery getScoreDiscovery() {
        return this.scoreDiscovery;
    }

    @Override
    public JSessionContext getSystemSessionContext() {
        return SYSTEM_SESSION;
    }

    @Override
    JSessionContext sessionContext(String userId, String sessionId) {
        return new JSessionContext(userId, sessionId);
    }

}
