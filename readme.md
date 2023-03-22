# What Is the Fastest Way to Write to a Sqlite Database From Java in 2023?

## Background 
Our challenge was to insert data into a sqlite database as quickly as possible.
You can find a lot of information on insert performance on the internet. Most information
is already a couple of years old. Sqlite has improved. The drivers have improved. Java has improved.

A lot of information we found was no longer accurate or was slower than not tuning anything at all.

We therefore benchmarked the insert performance ourselves.
Our findings are a bit surprising and are different to what is currently recommended
on e.g. Stack Overflow (https://stackoverflow.com/questions/1711631/improve-insert-per-second-performance-of-sqlite).

## Setup
- Artificial data. 5 columns with text should be inserted 5 million times.
- We ran the benchmark on a Mac M1 Laptop.
- Output is a sqlite database with 160MB.
- sqlite-jdbc-3.41.0.1.
- Java OpenJDK 19.

You can find the code at https://github.com/raphaelbauer/-java-sqlite-insert-benchmark and a post on my blog at https://www.raphaelbauer.com/posts/fastest-way-to-write-to-sqlite-from-java-in-2023/.
Simply check out the repository and run the file BenchmarkOptionsToInsertIntoSqlite.java. 
All code is in that one file.

## Limits
- That's not a real benchmark like JMH. But as rule of thumb it should work fine.
- You might get different results with different OSses and filesystems.

## Conclusion
- Use one large transaction.
- Statements are a bit faster than prepared statements - but for all practical purposes you should use prepared statements to omit SQL injections.
- Configuration of SQLite (synchronization mode, memory and such) did not bring any measurable benefit.
- Batching did NOT bring any benefit.

## Results and Insert Performance
- No transaction was 1000 times slower than a single transaction.
- A simple statement in one big transaction can write 833_333 rows per second on my machine.
- A prepared statement in one big transaction can write 714_285 rows per second on my machine.
- A prepared statement in one big transaction with batches of 1000 can write 500_000 rows per second on my machine.

Some notes:
- It is surprising that batching is slower than not batching. We tried different batch sizes (1000, 10000, 100000) - it does not change much.
- We tried many PRAGMA configurations on sqlite level. We could not find any big difference using any of the PRAGMA configs.

## Questions

### Question: What is the fastest way to write to a SQLite database?
- Simple statements, one transaction, no batches.
- For practical reasons - because you don't want to have SQL injections - you should use simple prepared statements, one transaction, no batches.

### Question: Do SQlite configuration options give you any edge?
In short: No.

We tried different variations of the following:

    SQLiteConfig config = new SQLiteConfig();
    config.setJournalMode(SQLiteConfig.JournalMode.OFF);
    config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
    config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
    config.setTempStore(SQLiteConfig.TempStore.MEMORY);
    config.setPragma(SQLiteConfig.Pragma.MMAP_SIZE, "30000000000");

It did not change anything significantly in terms of write performance.

### Question: Can't we just parallelize the work?
Nope. An insert will lock the table. You can only use one connection to write to the database at a given times.
Multiple write efforts will block each other.

### Question: What has the most impact on performance?
Using one transaction.

Do all insert commands within one huge transaction. Not using a transaction is 1000x slower than the other way round.
Absolutely use something like connection.setAutocommit(false) - and then connection.commit() at the end.
