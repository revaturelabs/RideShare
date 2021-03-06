package com.revature.rideshare.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.revature.rideshare.domain.AvailableRide;
import com.revature.rideshare.domain.Car;
import com.revature.rideshare.domain.PointOfInterest;
import com.revature.rideshare.domain.RideRequest;
import com.revature.rideshare.domain.RideRequest.RequestStatus;
import com.revature.rideshare.domain.User;
import com.revature.rideshare.json.Action;
import com.revature.rideshare.json.Attachment;
import com.revature.rideshare.json.SlackJSONBuilder;

/**
 * Used for sending constructing/sending slack messages and parsing incoming
 * slack messages. <br>
 * <br>
 * Uses SlackJSONBuilder structure to dynamically create slack messages with
 * nested options and actions in attachment groups. <br>
 * <br>
 * The attachments are then inserted into the SlackJSONBuilder which is
 * converted into the message format required by slack (in String format.) <br>
 * <br>
 * Message payloads are parsable version of slack's response to a user's
 * interaction with a slack message. *<br>
 * <br>
 * Message callbackIds should be the same as their creation name for reflection
 * invocation purposes. <br>
 * If a message's template can't be built from its payload, the message should
 * have a String-arg constructor with the same name to return logic comparison
 * <br>
 * (see methods {@link SlackServiceImpl#handleMessage handleMessage},
 * {@link SlackServiceImpl#isMessageActionable isMessageActionable})
 * 
 * @since 7/22/2017
 * @author Mark Worth
 * @author Gian-Carlo Barreto
 * @author Dylan McBee
 */
@Component("slackService")
@Transactional
public class SlackServiceImpl implements SlackService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SlackMessageService slackMessageService;

	@Autowired
	private RideService rideService;

	@Autowired
	private CarService carService;

	@Autowired
	private UserService userService;

	@Autowired
	private PointOfInterestService poiService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#setSlackMessageService(com.
	 * revature.rideshare.service.SlackMessageService)
	 */
	@Override
	public void setSlackMessageService(SlackMessageService slackMessageService) {
		this.slackMessageService = slackMessageService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#setCarService(com.revature.
	 * rideshare.service.CarService)
	 */
	@Override
	public void setCarService(CarService carService) {
		this.carService = carService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#setUserService(com.revature.
	 * rideshare.service.UserService)
	 */
	@Override
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#setRideService(com.revature.
	 * rideshare.service.RideService)
	 */
	@Override
	public void setRideService(RideService rideService) {
		this.rideService = rideService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#setPoiService(com.revature.
	 * rideshare.service.PointOfInterestService)
	 */
	@Override
	public void setPoiService(PointOfInterestService poiService) {
		this.poiService = poiService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#newRideMessage(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public String newRideMessage(String userId, String text) {
		ObjectMapper mapper = new ObjectMapper();
		String date = slackMessageService.getDateFromText(text);
		ArrayList<Attachment> attachments = new ArrayList<Attachment>();
		String callbackId = "newRideMessage";

		// Creating the attachments
		Attachment fromPOIAttachment = slackMessageService.createPOIAttachment("From Destination", callbackId);
		Attachment toPOIAttachment = slackMessageService.createPOIAttachment("To Destination", callbackId);
		Attachment seatsAttachment = slackMessageService.createSeatsAttachment(callbackId);

		attachments.add(slackMessageService.createTimeAttachment(callbackId));
		attachments.add(fromPOIAttachment);
		attachments.add(toPOIAttachment);
		attachments.add(seatsAttachment);
		attachments.add(slackMessageService.createConfirmationButtonsAttachment(callbackId));

		SlackJSONBuilder rr = new SlackJSONBuilder(userId, "New ride for " + date, "in_channel", attachments);
		rr.addDelimiters();

		String rideMessage = "";
		try {
			rideMessage = mapper.writeValueAsString(rr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rideMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#newRequestMessage(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public String newRequestMessage(String userId, String text) {
		ObjectMapper mapper = new ObjectMapper();
		String date = slackMessageService.getDateFromText(text);
		ArrayList<Attachment> attachments = new ArrayList<Attachment>();
		String callbackId = "newRequestMessage";

		// Creating the attachments
		Attachment fromPOIAttachment = slackMessageService.createPOIAttachment("From Destination", callbackId);
		Attachment toPOIAttachment = slackMessageService.createPOIAttachment("To Destination", callbackId);

		attachments.add(slackMessageService.createTimeAttachment(callbackId));
		attachments.add(fromPOIAttachment);
		attachments.add(toPOIAttachment);
		attachments.add(slackMessageService.createConfirmationButtonsAttachment(callbackId));

		SlackJSONBuilder rr = new SlackJSONBuilder(userId, "Ride request for " + date, "in_channel", attachments);
		rr.addDelimiters();

		String requestMessage = "";
		try {
			requestMessage = mapper.writeValueAsString(rr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return requestMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#findRidesMessage(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public String findRidesMessage(String userId, String text) {
		ObjectMapper mapper = new ObjectMapper();
		String date = slackMessageService.getDateFromText(text);
		ArrayList<Attachment> attachments = new ArrayList<Attachment>();
		String callbackId = "findRidesMessage";
		// Creating the attachments
		Attachment toFromPOIAttachment = slackMessageService.createPoiSelectDestinationAttachment(callbackId);
		Attachment startTimeAttachment = slackMessageService.createTimeAttachment(callbackId);
		Attachment endTimeAttachment = slackMessageService.createTimeAttachment(callbackId);
		startTimeAttachment.setText("Select start time.");
		endTimeAttachment.setText("Set end time.");
		attachments.add(toFromPOIAttachment);
		attachments.add(startTimeAttachment);
		attachments.add(endTimeAttachment);
		attachments.add(slackMessageService.createConfirmationButtonsAttachment(callbackId));
		SlackJSONBuilder rr = new SlackJSONBuilder(userId, "Ride request for " + date, "in_channel", attachments);
		rr.addDelimiters();

		String ridesMessage = "";
		try {
			ridesMessage = mapper.writeValueAsString(rr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ridesMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#findRequestsMessage(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public String findRequestsMessage(String userId, String date) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.revature.rideshare.service.SlackService#createRideByMessage(org.
	 * codehaus.jackson.JsonNode)
	 */
	@Override
	public String createRideByMessage(JsonNode payload) {
		String userId = payload.path("user").path("id").asText();
		User user = userService.getUserBySlackId(userId);
		Car userCar = carService.getCarForUser(user);
		if (userCar != null) {
			SlackJSONBuilder slackMessage = slackMessageService.convertPayloadToSlackJSONBuilder(payload);
			List<String> strings = new ArrayList<String>();
			strings = slackMessageService.getTextFields(slackMessage);
			String dateString = strings.get(0);
			String hour = strings.get(1);
			String minute = strings.get(2);
			String meridian = strings.get(3);
			String pickupName = strings.get(4);
			String dropoffName = strings.get(5);
			Date time = slackMessageService.createRideDate(dateString, hour, minute, meridian);
			short seatsAvailable = Short.parseShort(strings.get(6));
			PointOfInterest pickupPOI = poiService.getPoi(pickupName);
			PointOfInterest dropoffPOI = poiService.getPoi(dropoffName);
			AvailableRide availableRide = new AvailableRide();
			availableRide.setCar(userCar);
			availableRide.setPickupPOI(pickupPOI);
			availableRide.setDropoffPOI(dropoffPOI);
			availableRide.setSeatsAvailable(seatsAvailable);
			availableRide.setOpen(true);
			availableRide.setTime(time);
			availableRide.setNotes("");
			rideService.addOffer(availableRide);
			String confirmationMessage = "Your ride for " + time.toString() + " from " + pickupName + " to "
					+ dropoffName + " with " + seatsAvailable + " seats  has been created";
			return confirmationMessage;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#createRequestByMessage(org.
	 * codehaus.jackson.JsonNode)
	 */
	@Override
	public String createRequestByMessage(JsonNode payload) {
		String userId = payload.path("user").path("id").asText();
		User user = userService.getUserBySlackId(userId);
		SlackJSONBuilder slackMessage = slackMessageService.convertPayloadToSlackJSONBuilder(payload);
		List<String> values = new ArrayList<String>();
		values = slackMessageService.getTextFields(slackMessage);
		values.forEach(v -> System.out.println("Value: " + v));
		String date = values.get(0);
		String hour = values.get(1);
		String minutes = values.get(2);
		String meridian = values.get(3);
		Date time = slackMessageService.createRideDate(date, hour, minutes, meridian);
		String fromPOI = values.get(4);
		String toPOI = values.get(5);
		RideRequest rideRequest = new RideRequest();
		rideRequest.setUser(user);
		rideRequest.setStatus(RequestStatus.OPEN);
		rideRequest.setPickupLocation(poiService.getPoi(fromPOI));
		rideRequest.setDropOffLocation(poiService.getPoi(toPOI));
		rideRequest.setTime(time);
		boolean success = rideService.addRequest(rideRequest);
		if (success) {
			String confirmationMessage = "Your ride request for " + time.toString() + " from " + fromPOI + " to "
					+ toPOI + " has been created";
			return confirmationMessage;
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#foundRidesByMessage(java.lang
	 * .String)
	 */
	@Override
	public boolean foundRidesByMessage(String message) {
		SlackJSONBuilder cMessage = slackMessageService.convertMessageStringToSlackJSONBuilder(message);
		if (cMessage.getAttachments().get(0).getActions().get(0).getText().equals("Select from the following rides")) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.revature.rideshare.service.SlackService#foundRidesByMessage(org.
	 * codehaus.jackson.JsonNode)
	 */
	@Override
	public String foundRidesByMessage(JsonNode payload) {
		ObjectMapper mapper = new ObjectMapper();
		String userId = slackMessageService.getUserId(payload);
		List<Attachment> attachments = new ArrayList<Attachment>();
		String callbackId = "foundRidesByMessage";
		List<String> strings = slackMessageService.getTextFields(payload);
		String dateString = strings.get(0);
		String filter = strings.get(1);
		String poiName = strings.get(2);
		String startHour = strings.get(3);
		String startMinute = strings.get(4);
		String startMeridian = strings.get(5);
		String endHour = strings.get(6);
		String endMinute = strings.get(7);
		String endMeridian = strings.get(8);
		Date startTime = slackMessageService.createRideDate(dateString, startHour, startMinute, startMeridian);
		Date endTime = slackMessageService.createRideDate(dateString, endHour, endMinute, endMeridian);
		// Creating the attachments
		System.out.println("start time: " + startTime);
		System.out.println("end time: " + endTime);
		Attachment availableRideAttachment = slackMessageService.createAvailableRidesAttachment(startTime, endTime,
				filter, poiName, callbackId);

		attachments.add(availableRideAttachment);
		attachments.add(slackMessageService.createConfirmationButtonsAttachment(callbackId));
		if (attachments.get(0).getActions().get(0).getOptions().size() == 0) {
			return "{\"replace_original\":\"true\",\"text\":\"" + "No rides matching that time were found." + "\"}";
		}
		SlackJSONBuilder rr = new SlackJSONBuilder(userId, "Matching rides for " + dateString, "in_channel",
				attachments);
		rr.addDelimiters();

		String requestMessage = "";
		try {
			requestMessage = mapper.writeValueAsString(rr);
		} catch (IOException e) {
			logger.error("Exception occurred when adding request through slack integration.");
		}
		return requestMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#foundRequestsByMessage(org.
	 * codehaus.jackson.JsonNode)
	 */
	@Override
	public String foundRequestsByMessage(JsonNode payload) {
		return "Message not implemented";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.revature.rideshare.service.SlackService#
	 * convertMessageRequestToPayload(java.lang.String)
	 */
	@Override
	public JsonNode convertMessageRequestToPayload(String request) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			request = URLDecoder.decode(request, "UTF-8");
			request = request.substring(8);
			JsonNode payload = mapper.readTree(request);
			System.out.println("payload is " + payload);
			return payload;
		} catch (IOException e) {
			logger.error("Payload conversion exception");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#handleMessage(org.codehaus.
	 * jackson.JsonNode)
	 */
	@Override
	public String handleMessage(JsonNode payload) {
		String callbackId = payload.path("callback_id").asText();
		ObjectMapper mapper = new ObjectMapper();
		String currentMessage = payload.path("original_message").toString();
		SlackJSONBuilder cMessage;
		try {
			cMessage = mapper.readValue(currentMessage, SlackJSONBuilder.class);
			List<String> strings = slackMessageService.getTextFields(cMessage);
			boolean isNewRequestOrRide = (callbackId.equals("newRideMessage")
					|| callbackId.equals("newRequestMessage"));
			if (isNewRequestOrRide && strings.get(4).equals(strings.get(5))) {
				return ("Invalid Selection: Cannot use matching origin and destination.");
			}
			boolean isFindRequestOrRide = (callbackId.equals("findRidesMessage")
					|| callbackId.equals("findRequestsMessage"));
			if (isFindRequestOrRide && strings.get(6).equals(strings.get(7))) {
				return ("Invalid Selection: Cannot use matching origin and destination");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		switch (callbackId) {
		case ("newRideMessage"):
			return createRideByMessage(payload);
		case ("newRequestMessage"):
			return createRequestByMessage(payload);
		case ("findRidesMessage"):
			return foundRidesByMessage(payload);
		case ("findRequestsMessage"):
			return foundRequestsByMessage(payload);
		case ("foundRequestsByMessage"):
			return addPassengersToRideByMessage(payload);
		case ("foundRidesByMessage"):
			return addUserToRideByMessage(payload);
		default:
			return "Message does not match any known callbackid, callbackId is " + callbackId;
		}
	}

	/**
	 * --Not implemented, only here for naming clarity-- Should be used for a
	 * driver to add passengers to their ride.
	 * 
	 * @param JsonNode
	 *            payload
	 * @return Confirmation message to be propagated as a message to slack user.
	 */
	private String addPassengersToRideByMessage(JsonNode payload) {
		return "Message not implemented";
	}

	/**
	 * Adds a user to a ride selected through slack integration messages.
	 * 
	 * @param JsonNode
	 *            payload
	 * @return String Confirmation message to be propagated as a message to
	 *         slack user.
	 */
	private String addUserToRideByMessage(JsonNode payload) {
		SlackJSONBuilder message = slackMessageService.convertPayloadToSlackJSONBuilder(payload);
		String userId = slackMessageService.getUserId(payload);
		String rideInfo = message.getAttachments().get(0).getActions().get(0).getText();
		long rideId = Long.parseLong(rideInfo.split(":")[rideInfo.split(":").length - 1]);
		User u = userService.getUserBySlackId(userId);
		AvailableRide ride = rideService.getRideById(rideId);
		Date time = ride.getTime();
		String fromPOI = ride.getPickupPOI().getPoiName();
		String toPOI = ride.getDropoffPOI().getPoiName();
		boolean addedUser = rideService.acceptOffer(rideId, u);
		String confirmationMessage;
		if (addedUser) {
			confirmationMessage = "Your ride request for " + time.toString() + " from " + fromPOI + " to " + toPOI
					+ " has been created";
		} else {
			confirmationMessage = "There was a problem with your request. Please try again.";
		}
		return confirmationMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.revature.rideshare.service.SlackService#isMessageActionable(org.
	 * codehaus.jackson.JsonNode)
	 */
	@Override
	public boolean isMessageActionable(JsonNode payload) {
		String callbackId = payload.path("callback_id").asText();
		String currentMessage = payload.path("original_message").toString();
		String userId = payload.path("user").path("id").asText();
		String text = payload.path("original_message").path("text").asText();
		String date = text.split(" ")[text.split(" ").length - 1];
		Method method;
		try {
			String template = "";
			if (slackMessageService.templateCanBeBuiltFromPayload(callbackId)) {
				method = this.getClass().getMethod(callbackId, userId.getClass(), date.getClass());
				template = (String) method.invoke(this, userId, date);
				return compareMessages(currentMessage, template);
			} else {// returns the boolean without template comparison(requires
					// direct logic in named method[String-arg])
				method = this.getClass().getMethod(callbackId, currentMessage.getClass());
				return (Boolean) method.invoke(this, currentMessage);
			}
		} catch (SecurityException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException ex) {
			logger.error("Reflection call error", ex);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#compareMessages(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public boolean compareMessages(String currentMessage, String template) {
		SlackJSONBuilder cMessage = slackMessageService.convertMessageStringToSlackJSONBuilder(currentMessage);
		SlackJSONBuilder tMessage = slackMessageService.convertMessageStringToSlackJSONBuilder(template);
		List<Attachment> cAttachments = cMessage.getAttachments();
		List<Attachment> tAttachments = tMessage.getAttachments();
		for (int i = 0; i < cAttachments.size(); i++) {
			List<Action> cActions = cAttachments.get(i).getActions();
			List<Action> tActions = tAttachments.get(i).getActions();
			for (int j = 0; j < cActions.size(); j++) {
				String type = cActions.get(j).getType();
				if (type.equals("select")) {
					if (cActions.get(j).getText().equals(tActions.get(j).getText())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.revature.rideshare.service.SlackService#isMessageEndOfBranch(java.
	 * lang.String)
	 */
	@Override
	public boolean isMessageEndOfBranch(String callbackId) {
		return callbackId.equals("newRideMessage") || callbackId.equals("newRequestMessage")
				|| callbackId.equals("foundRidesByMessage") || callbackId.equals("foundRequestsByMessage");
	}
}