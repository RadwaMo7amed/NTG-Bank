package com.NTGBank.configuration;

import com.NTGBank.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

//it implements ItemProcessor <input,output>

public class CustomerProcessor implements ItemProcessor<Customer,Customer> {
    @Override
    //At this we write the filter we need to apply at data
    public Customer process(Customer item) throws Exception {
        //But At this we don't need any filter at this
        return item;
    }
}
