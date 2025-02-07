package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class PaNotificationLimitEntity {
    public static final String FIELD_PK = "pk";
    public static final String FIELD_PAID = "paId";
    public static final String FIELD_YEAR_MONTH = "yearMonth";
    public static final String FIELD_REQUESTED_LIMIT = "requestedLimit";
    public static final String FIELD_RESIDUAL_LIMIT = "residualLimit";
    public static final String FIELD_DAILY_COUNTER_01 = "dailyCounter01";
    public static final String FIELD_DAILY_COUNTER_02 = "dailyCounter02";
    public static final String FIELD_DAILY_COUNTER_03 = "dailyCounter03";
    public static final String FIELD_DAILY_COUNTER_04 = "dailyCounter04";
    public static final String FIELD_DAILY_COUNTER_05 = "dailyCounter05";
    public static final String FIELD_DAILY_COUNTER_06 = "dailyCounter06";
    public static final String FIELD_DAILY_COUNTER_07 = "dailyCounter07";
    public static final String FIELD_DAILY_COUNTER_08 = "dailyCounter08";
    public static final String FIELD_DAILY_COUNTER_09 = "dailyCounter09";
    public static final String FIELD_DAILY_COUNTER_10 = "dailyCounter10";
    public static final String FIELD_DAILY_COUNTER_11 = "dailyCounter11";
    public static final String FIELD_DAILY_COUNTER_12 = "dailyCounter12";
    public static final String FIELD_DAILY_COUNTER_13 = "dailyCounter13";
    public static final String FIELD_DAILY_COUNTER_14 = "dailyCounter14";
    public static final String FIELD_DAILY_COUNTER_15 = "dailyCounter15";
    public static final String FIELD_DAILY_COUNTER_16 = "dailyCounter16";
    public static final String FIELD_DAILY_COUNTER_17 = "dailyCounter17";
    public static final String FIELD_DAILY_COUNTER_18 = "dailyCounter18";
    public static final String FIELD_DAILY_COUNTER_19 = "dailyCounter19";
    public static final String FIELD_DAILY_COUNTER_20 = "dailyCounter20";
    public static final String FIELD_DAILY_COUNTER_21 = "dailyCounter21";
    public static final String FIELD_DAILY_COUNTER_22 = "dailyCounter22";
    public static final String FIELD_DAILY_COUNTER_23 = "dailyCounter23";
    public static final String FIELD_DAILY_COUNTER_24 = "dailyCounter24";
    public static final String FIELD_DAILY_COUNTER_25 = "dailyCounter25";
    public static final String FIELD_DAILY_COUNTER_26 = "dailyCounter26";
    public static final String FIELD_DAILY_COUNTER_27 = "dailyCounter27";
    public static final String FIELD_DAILY_COUNTER_28 = "dailyCounter28";
    public static final String FIELD_DAILY_COUNTER_29 = "dailyCounter29";
    public static final String FIELD_DAILY_COUNTER_30 = "dailyCounter30";
    public static final String FIELD_DAILY_COUNTER_31 = "dailyCounter31";

    public static final String INDEX_YEAR_MONTH = "yearMonth-index";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PAID)})) private String paId;
    @Getter(onMethod=@__({@DynamoDbSecondaryPartitionKey(indexNames = {INDEX_YEAR_MONTH}), @DynamoDbAttribute(FIELD_YEAR_MONTH)})) private String yearMonth;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_REQUESTED_LIMIT)})) private Integer requestedLimit;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RESIDUAL_LIMIT)})) private Integer residualLimit;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_01)})) private Integer dailyCounter01;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_02)})) private Integer dailyCounter02;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_03)})) private Integer dailyCounter03;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_04)})) private Integer dailyCounter04;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_05)})) private Integer dailyCounter05;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_06)})) private Integer dailyCounter06;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_07)})) private Integer dailyCounter07;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_08)})) private Integer dailyCounter08;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_09)})) private Integer dailyCounter09;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_10)})) private Integer dailyCounter10;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_11)})) private Integer dailyCounter11;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_12)})) private Integer dailyCounter12;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_13)})) private Integer dailyCounter13;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_14)})) private Integer dailyCounter14;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_15)})) private Integer dailyCounter15;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_16)})) private Integer dailyCounter16;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_17)})) private Integer dailyCounter17;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_18)})) private Integer dailyCounter18;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_19)})) private Integer dailyCounter19;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_20)})) private Integer dailyCounter20;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_21)})) private Integer dailyCounter21;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_22)})) private Integer dailyCounter22;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_23)})) private Integer dailyCounter23;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_24)})) private Integer dailyCounter24;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_25)})) private Integer dailyCounter25;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_26)})) private Integer dailyCounter26;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_27)})) private Integer dailyCounter27;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_28)})) private Integer dailyCounter28;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_29)})) private Integer dailyCounter29;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_30)})) private Integer dailyCounter30;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_31)})) private Integer dailyCounter31;
}
