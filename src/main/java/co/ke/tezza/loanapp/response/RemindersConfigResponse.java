package co.ke.tezza.loanapp.response;

import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.Days;
import co.ke.tezza.loanapp.enums.ReminderFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemindersConfigResponse {
	private long reminderId;

	@Enumerated(EnumType.STRING)
	private ReminderFrequency reminderFrequency;

	private Integer maxReminders;

	private int startNoOfDaysBefore;
	private int startNoOfDaysAfter;

	private SmsSetupResponse smsTemplate;
	private String sendTime; 
    private Boolean sendTimeEnabled; 
    private String timezone; 
    private List<String> sendTimes; 
    private Boolean useMultipleTimes; 
    private List<Days> specificDays;
	private boolean isActive;

}
