package com.banking_system.api_server;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServerApplication.class, args);

		StandardPBEStringEncryptor pbeStringEncryptor = new StandardPBEStringEncryptor();
		pbeStringEncryptor.setAlgorithm("PBEWithMD5AndDES");
		pbeStringEncryptor.setPassword("hyunho");

		String DB_URL = pbeStringEncryptor.encrypt("jdbc:oracle:thin:@118.67.134.56:1521:xe");
		String USERNAME = pbeStringEncryptor.encrypt("hyunho");
		String PW = pbeStringEncryptor.encrypt("1111");

		System.out.println("DB_URL :: encrypt :: " + DB_URL);
		System.out.println("DB_URL :: USERNAME :: " + USERNAME);
		System.out.println("DB_URL :: PW :: " + PW);
	}

}
