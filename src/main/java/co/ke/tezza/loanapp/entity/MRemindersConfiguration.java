package co.ke.tezza.loanapp.entity;

import javax.persistence.*;

import co.ke.tezza.loanapp.enums.Days;
import co.ke.tezza.loanapp.enums.ReminderFrequency;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Reminder_Config")
public class MRemindersConfiguration extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Reminder_Config_ID")
	private long reminderId;

	@Column(name = "reminder_frequency")
	@Enumerated(EnumType.STRING)
	private ReminderFrequency reminderFrequency;

	@Column(name = "max_reminders")
	private Integer maxReminders;

	@Column(name = "start_no_of_days_before")
	private int startNoOfDaysBefore;

	@Column(name = "start_no_of_days_after")
	private int startNoOfDaysAfter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AD_Sms_Setup_ID", nullable = false)
	private MSmsSetup smsMessageTemplate;

	// Time configuration fields
	@Column(name = "send_time")
	private String sendTime;

	@Column(name = "send_time_enabled")
	private Boolean sendTimeEnabled;

	@Column(name = "timezone")
	private String timezone;

	@ElementCollection
	@CollectionTable(name = "AD_Reminder_Send_Times", joinColumns = @JoinColumn(name = "AD_Reminder_Config_ID"))
	@Column(name = "send_time_value")
	private List<String> sendTimes;

	@Column(name = "use_multiple_times")
	private Boolean useMultipleTimes;
	
	@ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "AD_Reminder_Specific_Days",
        joinColumns = @JoinColumn(name = "AD_Reminder_Config_ID")
    )
    @Column(name = "day_of_week")
    private List<Days> specificDays;

}