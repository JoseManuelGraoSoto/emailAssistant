package com.email.emailSP;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
    private float temperature;

}
