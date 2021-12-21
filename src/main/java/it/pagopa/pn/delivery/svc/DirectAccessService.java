package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.commons_delivery.middleware.DirectAccessTokenDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class DirectAccessService {

	private final DirectAccessTokenDao directAccessTokenDao;
	private final RecipientsChallenge recipientsChallenge;

	@Autowired
	public DirectAccessService(DirectAccessTokenDao directAccessTokenDao,
							   RecipientsChallenge recipientsChallenge) {
		this.directAccessTokenDao = directAccessTokenDao;
		this.recipientsChallenge = recipientsChallenge;
	}

	public Optional<DirectAccessToken> doChallenge( String directAccessToken, String userSecret ) {
		log.debug( "Retrieve direct access token info for token={}", directAccessToken );
		Optional<DirectAccessToken> directAccessTokenInfo = directAccessTokenDao.getDirectAccessToken( directAccessToken );
		if (directAccessTokenInfo.isPresent()) {
			log.debug( "Retrieve secret START" );
			String secret = recipientsChallenge.getSecret( directAccessTokenInfo.get().getTaxId() );
			if (!secret.equals(userSecret)) {
				log.error( "User specify different secret for direct access token={}", directAccessToken );
				directAccessTokenInfo = Optional.empty();
			}
		} else {
			log.info( "Unable to retrieve direct access token info for direct access token={}", directAccessToken );
			directAccessTokenInfo = Optional.empty();
		}
		return directAccessTokenInfo;
	}




}
