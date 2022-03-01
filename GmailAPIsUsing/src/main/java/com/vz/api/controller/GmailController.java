package com.vz.api.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vz.api.dto.Mail;
import com.vz.api.service.GmailRefreshService;

@RestController
@RequestMapping("/gmail")
public class GmailController {

	@Autowired
	GmailRefreshService refreshService;
	
	@GetMapping("/refresh")
	public List<Mail> getNewMails() throws IOException, GeneralSecurityException {
		return refreshService.refreshMails();	
	}
}
