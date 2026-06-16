package co.ke.tezza.loanapp.service.bbb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class BigBlueButtonService {

	private final WebClient webClient;

	@Value("${bbb.api.url}")
	private String bbbApiUrl;

	@Value("${bbb.api.secret}")
	private String bbbSecret;

	public BigBlueButtonService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public Mono<String> createMeeting(String meetingID, String attendeePW, String moderatorPW, String meetingName,int durationHrs) {
		String callName = "create";

		String queryString = String.format("meetingID=%s&attendeePW=%s&moderatorPW=%s&name=%s&duration=%s", urlEncode(meetingID),
				urlEncode(attendeePW), urlEncode(moderatorPW), urlEncode(meetingName),durationHrs);

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);
		System.out.println("Checksum: " + checksum);
		System.out.println("Request URL: " + fullUrl);
		System.out.println("Encoded meetingName: " + urlEncode(meetingName));

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> joinMeeting(String meetingID, String fullName,  String role) {
	    String callName = "join";

	    String queryString = String.format("meetingID=%s&fullName=%s&role=%s",
	            urlEncode(meetingID), urlEncode(fullName), urlEncode(role));

	    String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
	    String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);
	    System.out.println("Checksum: " + checksum);
	    System.out.println("Request URL: " + fullUrl);

	    return webClient.get()
	            .uri(fullUrl)
	            .retrieve()
	            .bodyToMono(String.class);
	}


	public Mono<String> endMeeting(String meetingID, String moderatorPW) {
		String callName = "end";

		String queryString = String.format("meetingID=%s&password=%s", urlEncode(meetingID), urlEncode(moderatorPW));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> sendChatMessage(String meetingID, String message, String senderName) {
		String callName = "sendChatMessage";

		String queryString = String.format("meetingID=%s&message=%s&sender=%s", urlEncode(meetingID),
				urlEncode(message), urlEncode(senderName));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> insertDocument(String meetingID, List<String> documentUrls) {
		String callName = "insertDocument";

		String documentUrlsStr = String.join(",", documentUrls);
		String queryString = String.format("meetingID=%s&documentURLs=%s", urlEncode(meetingID),
				urlEncode(documentUrlsStr));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> isMeetingRunning(String meetingID) {
		String callName = "isMeetingRunning";

		String queryString = String.format("meetingID=%s", urlEncode(meetingID));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> getMeetings() {
		String callName = "getMeetings";

		String queryString = "";

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> getMeetingInfo(String meetingID) {
		String callName = "getMeetingInfo";

		String queryString = String.format("meetingID=%s", urlEncode(meetingID));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> getRecordings() {
		String callName = "getRecordings";

		String queryString = ""; // Empty for getRecordings call

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> publishRecordings(String recordingID, boolean publish) {
		String callName = "publishRecordings";

		String queryString = String.format("recordingID=%s&publish=%b", urlEncode(recordingID), publish);

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> deleteRecordings(String recordingID) {
		String callName = "deleteRecordings";

		String queryString = String.format("recordingID=%s", urlEncode(recordingID));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> updateRecordings(String recordingID, String metadata) {
		String callName = "updateRecordings";

		String queryString = String.format("recordingID=%s&metadata=%s", urlEncode(recordingID), urlEncode(metadata));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> getRecordingTextTracks(String recordingID) {
		String callName = "getRecordingTextTracks";

		String queryString = String.format("recordingID=%s", urlEncode(recordingID));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	public Mono<String> putRecordingTextTrack(String recordingID, String fileUrl) {
		String callName = "putRecordingTextTrack";

		String queryString = String.format("recordingID=%s&fileURL=%s", urlEncode(recordingID), urlEncode(fileUrl));

		String checksum = generateBBBChecksum(callName, queryString, bbbSecret);
		String fullUrl = String.format("%s/%s?%s&checksum=%s", bbbApiUrl, callName, queryString, checksum);

		return webClient.get().uri(fullUrl).retrieve().bodyToMono(String.class);
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String generateBBBChecksum(String callName, String queryString, String sharedSecret) {
		String data = callName + queryString + sharedSecret;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-1 algorithm not available", e);
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			hexString.append(String.format("%02x", b));
		}
		return hexString.toString();
	}

}
