

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.sqlite.SQLiteConfig;

public class BenchmarkOptionsToInsertIntoSqlite {

    private final static String OUTPUT_FILE = "output";
    private final static String CREATE_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS data (one TEXT, two TEXT, three TEXT, four TEXT, five TEXT)";
    private final static int NUMBER_OF_ROWS = 5_000_000;
    private final static String[] DUMMY_DATA = new String[]{"one", "two", "three", "four", "five"};

    public static void main(String[] a) throws Exception {

        // https://stackoverflow.com/questions/1711631/improve-insert-per-second-performance-of-sqlite
        var allBenchmarksToRun = new ArrayList<Benchmarkable>();
        //allBenchmarksToRun.add(new Benchmark_Simple_Statements_Without_Escaping_Many_Transactions());
        allBenchmarksToRun.add(new Benchmark_Simple_Statements_Without_Escaping_Single_Transaction());
        allBenchmarksToRun.add(new Benchmark_Prepared_Statements_Single_Transaction());
        allBenchmarksToRun.add(new Benchmark_Prepared_Statements_Single_Transaction_Batched_10000());

        runAllBenchmarkables(allBenchmarksToRun);

    }

    public static void runAllBenchmarkables(List<Benchmarkable> benchmarkables) throws Exception {

        for (Benchmarkable benchmarkable : benchmarkables) {
            var duration = benchmarkable.doIt();
            printResult(benchmarkable.getClass().getName(), duration);
        }

    }

    public static void printResult(String nameOfBenchmark, Duration duration) {

        double perSecond;

        if (duration.getSeconds() == 0) {
            perSecond = NUMBER_OF_ROWS; // let's prevent division by zero
        } else {
            perSecond = NUMBER_OF_ROWS / duration.getSeconds();
        }

        System.out.println(nameOfBenchmark + ": " + duration.toSeconds() + "s -- " + perSecond + " rows per second");
    }

    public static interface Benchmarkable {

        public Duration doIt() throws Exception;
    }

    public static class Benchmark_Simple_Statements_Without_Escaping_Many_Transactions implements Benchmarkable {

        public Benchmark_Simple_Statements_Without_Escaping_Many_Transactions() {
        }

        public Duration doIt() throws Exception {

            Instant start = Instant.now();

            SQLiteConfig config = new SQLiteConfig();

            try (Connection connection = config.createConnection("jdbc:sqlite:" + OUTPUT_FILE + System.currentTimeMillis() + ".sqlite")) {

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(CREATE_TABLE_STATEMENT);
                }

                for (int i = 0; i < NUMBER_OF_ROWS; i++) {

                    // That's not safe and should not be done in production
                    // Let's still get some benchmarks on it...
                    String insertStatement = "INSERT INTO data VALUES ("
                            + "'" + DUMMY_DATA[0] + "',"
                            + "'" + DUMMY_DATA[1] + "',"
                            + "'" + DUMMY_DATA[2] + "',"
                            + "'" + DUMMY_DATA[3] + "',"
                            + "'" + DUMMY_DATA[4] + "'"
                            + ")";

                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(insertStatement);
                    }

                }

            }

            Instant end = Instant.now();
            return Duration.between(start, end);

        }
    }

    public static class Benchmark_Simple_Statements_Without_Escaping_Single_Transaction implements Benchmarkable {

        public Benchmark_Simple_Statements_Without_Escaping_Single_Transaction() {
        }

        public Duration doIt() throws Exception {

            Instant start = Instant.now();

            SQLiteConfig config = new SQLiteConfig();
            config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
            config.setPragma(SQLiteConfig.Pragma.MMAP_SIZE, "468435456");
            config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
            config.setJournalMode(SQLiteConfig.JournalMode.OFF);
            config.setTempStore(SQLiteConfig.TempStore.MEMORY);

            try (Connection connection = config.createConnection("jdbc:sqlite:" + OUTPUT_FILE + System.currentTimeMillis() + ".sqlite")) {

                connection.setAutoCommit(false);

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(CREATE_TABLE_STATEMENT);
                }

                for (int i = 0; i < NUMBER_OF_ROWS; i++) {

                    String insertStatement = "INSERT INTO data VALUES ("
                            + "'" + DUMMY_DATA[0] + "',"
                            + "'" + DUMMY_DATA[1] + "',"
                            + "'" + DUMMY_DATA[2] + "',"
                            + "'" + DUMMY_DATA[3] + "',"
                            + "'" + DUMMY_DATA[4] + "'"
                            + ")";

                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(insertStatement);
                    }

                }

                connection.commit();

            }

            Instant end = Instant.now();
            return Duration.between(start, end);

        }
    }

    public static class Benchmark_Prepared_Statements_Single_Transaction implements Benchmarkable {

        public Benchmark_Prepared_Statements_Single_Transaction() {
        }

        public Duration doIt() throws Exception {

            Instant start = Instant.now();

            SQLiteConfig config = new SQLiteConfig();

            try (Connection connection = config.createConnection("jdbc:sqlite:" + OUTPUT_FILE + System.currentTimeMillis() + ".sqlite")) {

                connection.setAutoCommit(false);

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(CREATE_TABLE_STATEMENT);
                }

                String insertSql = "INSERT INTO data VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {

                    for (int i = 0; i < NUMBER_OF_ROWS; i++) {
                        preparedStatement.setString(1, DUMMY_DATA[0]);
                        preparedStatement.setString(2, DUMMY_DATA[1]);
                        preparedStatement.setString(3, DUMMY_DATA[2]);
                        preparedStatement.setString(4, DUMMY_DATA[3]);
                        preparedStatement.setString(5, DUMMY_DATA[4]);

                        preparedStatement.executeUpdate();

                    }

                }

                connection.commit();

            }

            Instant end = Instant.now();

            return Duration.between(start, end);

        }
    }

    public static class Benchmark_Prepared_Statements_Single_Transaction_Batched_10000 implements Benchmarkable {

        public Benchmark_Prepared_Statements_Single_Transaction_Batched_10000() {
        }

        public Duration doIt() throws Exception {

            Instant start = Instant.now();

            SQLiteConfig config = new SQLiteConfig();

            try (Connection connection = config.createConnection("jdbc:sqlite:" + OUTPUT_FILE + System.currentTimeMillis() + ".sqlite")) {

                connection.setAutoCommit(false);

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(CREATE_TABLE_STATEMENT);
                }

                String insertSql = "INSERT INTO data VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {

                    int batch_size = 10_000;
                    for (int i = 0; i < NUMBER_OF_ROWS; i++) {

                        preparedStatement.setString(1, DUMMY_DATA[0]);
                        preparedStatement.setString(2, DUMMY_DATA[1]);
                        preparedStatement.setString(3, DUMMY_DATA[2]);
                        preparedStatement.setString(4, DUMMY_DATA[3]);
                        preparedStatement.setString(5, DUMMY_DATA[4]);

                        preparedStatement.addBatch();

                        if (i % batch_size == 0 && i > 0) {
                            preparedStatement.executeBatch();
                        }

                    }

                    preparedStatement.executeBatch();

                }

                connection.commit();

            }

            Instant end = Instant.now();

            return Duration.between(start, end);

        }
    }

}
