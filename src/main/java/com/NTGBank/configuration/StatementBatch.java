package com.NTGBank.configuration;

import com.NTGBank.dto.AccountSummary;
import com.NTGBank.dto.Statement;
import com.NTGBank.entity.Customer;
import com.NTGBank.entity.Transaction;
import com.NTGBank.processor.StatementProcessor;
import com.NTGBank.repository.CustomerRepo;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;

@Configuration
@AllArgsConstructor
public class StatementBatch {

    private final DataSource dataSource;
    private final CustomerRepo customerRepo;
    private final StatementProcessor statementProcessor;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public RepositoryItemReader<Customer> statementItemReader() {
        RepositoryItemReader<Customer> itemReader = new RepositoryItemReader<>();
        itemReader.setRepository(customerRepo);
        itemReader.setMethodName("findAll"); // Updated method
        itemReader.setPageSize(10);
        itemReader.setSort(Collections.singletonMap("id", Sort.Direction.ASC));
        return itemReader;
    }
    @Bean
    public ItemProcessor<Customer, Statement> statementItemProcessor() {
        return customer -> statementProcessor.statementProcessor().process(customer);
    }

    @Bean
    public FlatFileItemWriter<Statement> statementWriter() {
        return new FlatFileItemWriterBuilder<Statement>()
                .name("statementWriter")
                .resource(new FileSystemResource("output/statements.txt")) // Output file path
                .lineAggregator(new LineAggregator<Statement>() {
                    @Override
                    public String aggregate(Statement statement) {
                        StringBuilder sb = new StringBuilder();

                        // Header
                        sb.append("\n                                       \n\n")
                                .append("NTG Bank Statement\n")
                                .append("\n                                   \n\n")
                                .append(String.format("Customer: %s\n", statement.getCustomerName()))
                                .append(String.format("Address: %s\n", statement.getCustomerAddress()))
                                .append(String.format("City: %s\n", statement.getCustomerCity()))
                                .append(String.format("State: %s\n", statement.getState()))
                                .append(String.format("PostalCode: %s\n", statement.getPostalCode()))
                                .append("\n                                      \n\n");

                        // Account summaries and transactions
                        for (AccountSummary accountSummary : statement.getAccountSummaries()) {
                            sb.append(String.format("Account ID: %d\n", accountSummary.getAccountId()))
                                    .append("Transactions:\n")
                                    .append("                                       \n");

                            for (Transaction transaction : accountSummary.getTransactions()) {
                                sb.append(transaction.toString()).append("\n");
                            }

                            sb.append("                                       \n")
                                    .append(String.format("Total Credits: %.2f\n", accountSummary.getTotalCredit()))
                                    .append(String.format("Total Debits: %.2f\n", accountSummary.getTotalDebit()))
                                    .append(String.format("Current Balance: %.2f\n", accountSummary.getCurrentBalance()))
                                    .append("=======================================================================================================================================================\n\n\n");
                        }
                        return sb.toString();
                    }
                })
                .build();
    }

    @Bean
    public Step statementStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("statementStep", jobRepository)
                .<Customer, Statement>chunk(10, transactionManager) // Chunk size of 10
                .reader(statementItemReader())
                .processor(statementItemProcessor())
                .writer(statementWriter())
                .allowStartIfComplete(true) // Allow restart if complete
                .taskExecutor(taskExecutor()) // Enable parallel processing
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(4); // Set concurrency limit to 4 threads
        return asyncTaskExecutor;
    }

    @Bean
    public Job statementJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("statementJob", jobRepository)
                .start(statementStep(jobRepository, transactionManager)) // Define the step
                .build();
    }
}