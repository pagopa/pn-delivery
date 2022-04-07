package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DirectAccessService {

	private final NotificationDao notificationDao;
	private final RecipientsChallenge recipientsChallenge;

	public DirectAccessService(NotificationDao notificationDao, RecipientsChallenge recipientsChallenge) {
		this.notificationDao = notificationDao;
		this.recipientsChallenge = recipientsChallenge;
	}
	
	public String generateToken(String iun, String taxId) {
		return iun + "_" + taxId + "_" + UUID.randomUUID();
	}
	
	public Optional<DirectAccessToken> doChallenge(String directAccessToken, String userSecret ) {
		log.info( "Retrieve direct access token info for token={}", directAccessToken );
		Optional<DirectAccessToken> directAccessTokenInfo = checkAndGetDirectAccessToken(directAccessToken);
		if (directAccessTokenInfo.isPresent()) {
			log.debug( "Retrieve secret START" );
			String secret = recipientsChallenge.getSecret( directAccessTokenInfo.get().getTaxId() );
			if (!secret.equals(userSecret)) {
				log.error( "User specify different secret for direct access token={}", directAccessToken );
				directAccessTokenInfo = Optional.empty();
			}
		} else {
			log.warn( "Unable to retrieve direct access token info for direct access token={}", directAccessToken );
			directAccessTokenInfo = Optional.empty();
		}
		return directAccessTokenInfo;
	}
	
	private Optional<DirectAccessToken> checkAndGetDirectAccessToken(String token) {
		String iun = getIunFromToken(token);
		
		if(iun != null){
			log.debug( "Get direct access token is for iun {}",iun );

			Optional<Notification> notificationOtp = notificationDao.getNotificationByIun(iun);
			if(notificationOtp.isPresent()){
				log.debug( "Notification is present for iun {}",iun );
				return getDirectAccessToken(token, iun, notificationOtp.get());
			}else{
				return handleInvalidToken(token, iun);
			}
		}else {
			return handleInvalidToken(token, null);
		}
	}

	private String getIunFromToken(String token) {
		return (token != null && token.contains("_")) ? token.substring(0, token.indexOf("_")) : null;
	}

	@NotNull
	private Optional<DirectAccessToken> getDirectAccessToken(String token, String iun, Notification notification) {

		Optional<String> taxIdOtp =
				notification.getRecipients().stream()
						.filter(recipient -> token.equals(recipient.getToken()))
						.map(NotificationRecipient::getTaxId).findFirst();

		if (taxIdOtp.isPresent()){
			log.debug( "TaxId is present for iun {}",iun );

			DirectAccessToken directAccessToken = DirectAccessToken.builder()
					.token(token)
					.iun(iun)
					.taxId(taxIdOtp.get())
					.build();
			
			return Optional.of(directAccessToken);
		}else {
			return handleInvalidToken(token, iun);
		}
	}

	private Optional<DirectAccessToken> handleInvalidToken(String token, String iun) {
		log.warn("Token {} is not valid for iun {}", token, iun);
		return Optional.empty();
	}

}
