package it.pagopa.pn.delivery.utils;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

@Component
public class ModelMapperFactory {

    public <S,D> ModelMapper createModelMapper( Class<S> sourceClass, Class<D> destinationClass ){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap( sourceClass, destinationClass );
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }
}
