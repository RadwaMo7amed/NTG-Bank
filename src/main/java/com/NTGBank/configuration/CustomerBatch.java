package com.NTGBank.configuration;

import com.NTGBank.entity.Account;
import com.NTGBank.entity.Customer;
import com.NTGBank.processor.CustomerProcessor;
import com.NTGBank.repository.CustomerRepo;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
@AllArgsConstructor
public class CustomerBatch {
    private CustomerRepo customerRepo;
    @Bean
    public FlatFileItemReader<Customer> customerItemReader(){
        //object//
        FlatFileItemReader<Customer>itemReader=new FlatFileItemReader<>();
        //Resource I will read from it
        itemReader.setResource(new FileSystemResource("src/main/resources/customerData.csv"));
        //name of it
        itemReader.setName("csv-reader");
        //skip first line it will be header
        itemReader.setLinesToSkip(1);
        //needed to tell java how the column name and how these separated
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    private LineMapper<Customer> lineMapper() {
        //it takes a tokenizer and filedMapper
        DefaultLineMapper<Customer> lineMapper=new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer=new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");//separator is comma
        tokenizer.setIncludedFields(0,1,2,3,4,5,6,7,8,9,10,11,12,13);
        tokenizer.setNames("customerId","accountId","firstName","middleName","lastName","address1","address2","city","state","postalCode","email","homePhone","callPhone","workPhone");//Header of csv file
        tokenizer.setStrict(false);
        lineMapper.setLineTokenizer(tokenizer);
        //assign to my class
        lineMapper.setFieldSetMapper(fieldSet -> {
            Customer customer=new Customer();
            customer.setCustomerId(Long.parseLong(fieldSet.readString("customerId")));
            customer.setFirstName(fieldSet.readString("firstName"));
            customer.setMiddleName(fieldSet.readString("middleName"));
            customer.setLastName(fieldSet.readString("lastName"));
            customer.setAddress1(fieldSet.readString("address1"));
            customer.setAddress2(fieldSet.readString("address2"));
            customer.setCity(fieldSet.readString("city"));
            customer.setState(fieldSet.readString("state"));
            customer.setPostalCode(fieldSet.readString("postalCode"));
            customer.setEmail(fieldSet.readString("email"));
            customer.setHomePhone(fieldSet.readString("homePhone"));
            customer.setCallPhone(fieldSet.readString("callPhone"));
            customer.setWorkPhone(fieldSet.readString("workPhone"));
            Account account=new Account();
            account.setAccountId(Long.parseLong(fieldSet.readString("accountId")));
            account.setCurrentBalance(0.0);
            account.setLastStatementDate(LocalDateTime.now());
            account.setCustomer(customer);


            customer.getAccounts().add(account);
            return customer;
        });
        return lineMapper;
    }

    @Bean
    public CustomerProcessor customerProcessor() {
        return new CustomerProcessor();
    }
    @Bean
    public RepositoryItemWriter<Customer> CustomerItemWriter(){
        //object
        RepositoryItemWriter<Customer>itemWriter=new RepositoryItemWriter<>();
        //Find The Repository
        itemWriter.setRepository(customerRepo);
        //we have to save the method write repo
        itemWriter.setMethodName("save");
        return itemWriter;
    }

    @Bean
    public Step customerStep(JobRepository jobRepository,
                             PlatformTransactionManager tx){
        return new StepBuilder("csv-Step",jobRepository)
                .<Customer,Customer>chunk(10,tx)
                .reader(customerItemReader())
                .processor((customerProcessor()))//it expected item processor which have <customer,customer> so we write at the chunk <customer,customer>//the processor we make 👆
                .writer(CustomerItemWriter())
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
    public Job customerJob (PlatformTransactionManager tx, JobRepository jobRepository){
        return new JobBuilder("customer-csv-Job",jobRepository)
                .flow(customerStep(jobRepository,tx))//the step we make 👇
                .end()
                .build();
    }
}
