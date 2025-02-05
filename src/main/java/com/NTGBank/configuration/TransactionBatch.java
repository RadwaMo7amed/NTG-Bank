package com.NTGBank.configuration;

import com.NTGBank.entity.Account;
import com.NTGBank.entity.Transaction;
import com.NTGBank.processor.TransactionProcessor;
import com.NTGBank.repository.AccountRepo;
import com.NTGBank.repository.CustomerRepo;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.beans.PropertyEditorSupport;
import java.net.BindException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class TransactionBatch {
    private final DataSource dataSource;
    private final AccountRepo accountRepo;
@Bean
public FlatFileItemReader<Transaction> transactionItemReader() {
    FlatFileItemReader<Transaction> reader = new FlatFileItemReader<>();
    reader.setLinesToSkip(1);
    reader.setResource(new FileSystemResource("src/main/resources/transactionData.csv"));

    DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
    DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("transactionId", "accountId", "description", "creditAmount", "debitAmount", "timestamp");

    lineMapper.setLineTokenizer(tokenizer);

    BeanWrapperFieldSetMapper<Transaction> fieldSetMapper = new BeanWrapperFieldSetMapper<>() {
        @Override
        public Transaction mapFieldSet(FieldSet fieldSet) {
            Transaction transaction = new Transaction();

            // Map simple fields
            transaction.setTransactionId(Long.parseLong(fieldSet.readString("transactionId")));
            transaction.setDescription(fieldSet.readString("description"));

            // Handle null or empty values for creditAmount and debitAmount
            fieldSet.readString("creditAmount");
            Double creditAmount = fieldSet.readString("creditAmount").isEmpty()
                    ? null : fieldSet.readDouble("creditAmount");
            fieldSet.readString("debitAmount");
            Double debitAmount = fieldSet.readString("debitAmount").isEmpty()
                    ? null : fieldSet.readDouble("debitAmount");
            transaction.setCreditAmount(creditAmount);
            transaction.setDebitAmount(debitAmount);

            // Handle LocalDateTime conversion
            String timestampStr = fieldSet.readString("timestamp");
            if (timestampStr.startsWith("'") && timestampStr.endsWith("'")) {
                timestampStr = timestampStr.substring(1, timestampStr.length() - 1); // Remove quotes
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");
            try {
                transaction.setTimestamp(LocalDateTime.parse(timestampStr, formatter));
            } catch (Exception e) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                transaction.setTimestamp(LocalDateTime.parse(timestampStr, formatter));
            }

            // Handle accountId to Account object conversion
            Long accountId = Long.parseLong(fieldSet.readString("accountId"));
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));
            transaction.setAccount(account);

            return transaction;
        }
    };

    lineMapper.setFieldSetMapper(fieldSetMapper);
    reader.setLineMapper(lineMapper);
    return reader;
}

    @Bean
    public TransactionProcessor transactionProcessor() {
        return new TransactionProcessor();
    }
    @Bean
    public JdbcBatchItemWriter<Transaction> transactionItemWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Transaction> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO transactions (credit_amount, debit_amount, account_id, transaction_id, timestamp, description) " +
                "VALUES (:creditAmount, :debitAmount, :account.accountId, :transactionId, :timestamp, :description)");
        writer.setItemSqlParameterSourceProvider(BeanPropertySqlParameterSource::new);
        return writer;
    }
    @Bean
    public Step transactionStep(JobRepository jobRepository,
                                PlatformTransactionManager tx){
        return new StepBuilder("csv-Step",jobRepository)
                .<Transaction,Transaction>chunk(10,tx)
                .reader(transactionItemReader())
                .processor((transactionProcessor()))//it expected item processor which have <customer,customer> so we write at the chunk <customer,customer>//the processor we make 👆
                .writer(transactionItemWriter(dataSource))
                .allowStartIfComplete(true)
                .taskExecutor(taskExecutor())//to make this task synchronize
                .build();
    }
    private TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor=new SimpleAsyncTaskExecutor();
        //set cryptocurrency how many trades should be given at this point
        asyncTaskExecutor.setConcurrencyLimit(1);
        return asyncTaskExecutor;
    }
    @Bean
    public Job transactionJob(PlatformTransactionManager tx, JobRepository jobRepository){
        return new JobBuilder("transaction-csv-Job",jobRepository)
                .flow(transactionStep(jobRepository,tx))//the step we make 👇
                .end()
                .build();
    }
}
