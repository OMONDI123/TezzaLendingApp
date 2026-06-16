package co.ke.tezza.loanapp.model.bbb;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BBBMeetingRequest {

	private String name="Austine";
	private String meetingID="1234";
	private String attendeePW = "attendeePW";
	private String moderatorPW = "moderatorPW";
	private String welcome = "Welcome to the meeting %%CONFNAME%%!";
	private String dialNumber = "123-456-7890";
	private String voiceBridge = "10000";
	private int maxParticipants = 1000;
	private String loginURL;
	private String logoutURL;
	private boolean record = false;
	private int duration = 0;
	private boolean isBreakout = false;
	private String parentMeetingID;
	private int sequence = 0;
	private boolean freeJoin = true;
	private boolean breakoutRoomsPrivateChatEnabled = true;
	private boolean breakoutRoomsRecord = false;
	private String meta;
	private String moderatorOnlyMessage;
	private boolean autoStartRecording = false;
	private boolean allowStartStopRecording = true;
	private boolean webcamsOnlyForModerator = false;
	private String bannerText;
	private String bannerColor = "#FFFFFF";
	private boolean muteOnStart = false;
	private boolean allowModsToUnmuteUsers = false;
	private boolean lockSettingsDisableCam = false;
	private boolean lockSettingsDisableMic = false;
	private boolean lockSettingsDisablePrivateChat = false;
	private boolean lockSettingsDisablePublicChat = false;
	private boolean lockSettingsDisableNotes = false;
	private boolean lockSettingsHideUserList = false;
	private boolean lockSettingsLockOnJoin = true;
	private boolean lockSettingsLockOnJoinConfigurable = false;
	private boolean lockSettingsHideViewersCursor = false;
	private String guestPolicy = "ALWAYS_ACCEPT";
	private boolean meetingKeepEvents = false;
	private boolean endWhenNoModerator = false;
	private int endWhenNoModeratorDelayInMinutes = 1;
	private String meetingLayout = "CUSTOM_LAYOUT";
	private int learningDashboardCleanupDelayInMinutes = 2;
	private boolean allowModsToEjectCameras = false;
	private boolean allowRequestsWithoutSession = false;
	private int userCameraCap = 3;
	private int meetingCameraCap = 0;
	private int meetingExpireIfNoUserJoinedInMinutes = 5;
	private int meetingExpireWhenLastUserLeftInMinutes = 1;
	private List<String> groups;
	private String logo;
	private String disabledFeatures;
	private String disabledFeaturesExclude;
	private boolean preUploadedPresentationOverrideDefault = true;
	private boolean notifyRecordingIsOn = false;
	private String presentationUploadExternalUrl;
	private String presentationUploadExternalDescription;
	private boolean presentationConversionCacheEnabled = false;
	private boolean recordFullDurationMedia = false;
	private String preUploadedPresentation;
	private String preUploadedPresentationName;
	private boolean allowOverrideClientSettingsOnCreateCall = false;
	private String clientSettingsOverride;
	private boolean allowPromoteGuestToModerator = false;
	private List<String> pluginManifests;
	private int maxNumPages = 200;

}
