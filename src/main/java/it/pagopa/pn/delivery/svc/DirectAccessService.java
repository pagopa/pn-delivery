package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.commons_delivery.middleware.DirectAccessTokenDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
		Optional<DirectAccessToken> directAccessTokenInfo = directAccessTokenDao.getDirectAccessToken( directAccessToken );
		if (directAccessTokenInfo.isPresent()) {
			String secret = recipientsChallenge.getSecret( directAccessTokenInfo.get().getTaxId() );
			if (!secret.equals(userSecret)) {
				directAccessTokenInfo = Optional.empty();
			}
		}
		return directAccessTokenInfo;
	}




}
