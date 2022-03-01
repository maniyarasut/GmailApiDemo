package com.vz.api.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.vz.api.dto.Mail;

import io.restassured.path.json.JsonPath;

@Service
public class GmailRefreshService {

	public static Long lstSyncTimestamp = LocalDateTime
			.parse("2022-01-01T00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).toEpochSecond(ZoneOffset.UTC);

	@Autowired
	GmailService gmailService;

	public List<Mail> refreshMails() throws IOException, GeneralSecurityException {
		List<Mail> mails = new ArrayList<Mail>();
		Gmail service = GmailService.getService();
		List<Label> labelList = gmailService.getLabels();
		List<String> impLabels = new ArrayList<String>();
		Map<String, String> labelMap = labelList.stream()
				.collect(Collectors.toMap(y -> y.getName().toLowerCase(), x -> x.getId()));
		impLabels.add(labelMap.get("inbox"));
		// impLabels.add(labelMap.get("newrelic"));

		/*
		 * long startTimeEpoch = LocalDateTime.parse("2021-12-27T23:00",
		 * DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
		 * .toEpochSecond(ZoneOffset.UTC);
		 */
		/*
		 * long endTimeEpoch = LocalDateTime.parse("2021-12-28T13:00",
		 * DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
		 * .toEpochSecond(ZoneOffset.UTC);
		 */
		String query = String.format("after:%s", lstSyncTimestamp);
		String account = "me";
		System.out.println(query);
		Long ts = 0L;
		ListMessagesResponse response = service.users().messages().list(account).setLabelIds(impLabels).setQ(query)
				.execute();
		List<Message> messages = new ArrayList<Message>();
		while (response.getMessages() != null) {
			messages.addAll(response.getMessages());
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken();
				response = service.users().messages().list(account).setLabelIds(impLabels).setQ(query)
						.setPageToken(pageToken).execute();
			} else {
				break;
			}
		}
		for (Message message : messages) {
			try {
				Message msg = service.users().messages().get("me", message.getId()).execute();
				// MessagePart payload = (MessagePart) msg.get("payload");
				// System.out.println(payload.get);
				JsonPath jp = new JsonPath(msg.toString());
				List<String> labels = msg.getLabelIds();
				String from = jp.getString("payload.headers.find { it.name == 'From' }.value");
				String to = jp.getString("payload.headers.find { it.name == 'To' }.value");
				String subject = jp.getString("payload.headers.find { it.name == 'Subject' }.value");
				String date = jp.getString("payload.headers.find { it.name == 'Date' }.value");
				// Date receivedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(date);
				// int beginIndex = date.indexOf(",");
				// int endIndex = date.indexOf("+")-1;
				Long timestamp =msg.getInternalDate()/1000;
				// String encBody = jp.getString("payload.parts[0].body.data");
				String emailBody = null;
				try {
					emailBody = StringUtils.newStringUtf8(
							Base64.getDecoder().decode(msg.getPayload().getParts().get(0).getBody().getData()));
					System.out.println(emailBody);
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
				if (timestamp > ts)
					ts = timestamp;
				Mail mail = new Mail();
				mail.setBody(emailBody);
				mail.setFrom(from);
				mail.setLabel(labels);
				mail.setSubject(subject);
				mail.setTo(to);
				mail.setReceivedDate(new Date(msg.getInternalDate()));
				mail.setId(msg.getId());
				mail.setThreadId(msg.getThreadId());
				mails.add(mail);
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
		if (ts > lstSyncTimestamp) {
			lstSyncTimestamp = ts+10L;
			System.out.println(new Date(lstSyncTimestamp*1000));
		}
		return mails;
	}
}
