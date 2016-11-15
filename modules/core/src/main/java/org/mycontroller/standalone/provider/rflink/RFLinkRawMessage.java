/*
 * Copyright 2015-2016 Jeeva Kandasamy (jkandasa@gmail.com)
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mycontroller.standalone.provider.rflink;

import org.mycontroller.standalone.AppProperties.NETWORK_TYPE;
import org.mycontroller.standalone.McObjectManager;
import org.mycontroller.standalone.gateway.IGateway;
import org.mycontroller.standalone.message.McMessage;
import org.mycontroller.standalone.message.McMessageUtils.MESSAGE_TYPE;
import org.mycontroller.standalone.message.McMessageUtils.MESSAGE_TYPE_INTERNAL;
import org.mycontroller.standalone.message.RawMessage;
import org.mycontroller.standalone.message.RawMessageException;
import org.mycontroller.standalone.provider.rflink.RFLinkUtils.RFLINK_MESSAGE_TYPE;

import lombok.Data;
import lombok.ToString;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 0.0.3
 */
@Data
@ToString
public class RFLinkRawMessage {

    private int gatewayId;
    private String nodeEui;
    private String sensorId;
    private MESSAGE_TYPE messageType;
    private String subType;
    private String payload;
    private Long timestamp;
    private boolean isTxMessage = false;
    private String protocol = null;

    public static final String KEY_PROTOCOL = "protocol";
    public static final String KEY_ID = "id";

    public RFLinkRawMessage(RawMessage rawMessage, String nodeEui, String key, String value)
            throws RawMessageException {
        this(rawMessage, nodeEui, null, key, value);
    }

    public RFLinkRawMessage(RawMessage rawMessage, String nodeEui, String protocol) throws RawMessageException {
        this(rawMessage, nodeEui, protocol, null, null);
    }

    private RFLinkRawMessage(RawMessage rawMessage, String nodeEui, String protocol, String key, String value)
            throws RawMessageException {
        IGateway gateway = McObjectManager.getGateway(rawMessage.getGatewayId());
        gatewayId = gateway.getGateway().getId();
        isTxMessage = rawMessage.isTxMessage();
        gatewayId = rawMessage.getGatewayId();
        timestamp = rawMessage.getTimestamp();
        if (protocol != null) {
            messageType = MESSAGE_TYPE.C_INTERNAL;
            subType = MESSAGE_TYPE_INTERNAL.I_PROPERTIES.getText();
            payload = KEY_PROTOCOL + "=" + protocol;
        } else if (key != null && value != null) {
            messageType = MESSAGE_TYPE.C_SET;
            RFLINK_MESSAGE_TYPE mType = RFLINK_MESSAGE_TYPE.valueOf(key.toUpperCase());
            subType = mType.getText();
            sensorId = key;
            switch (mType) {
                case SMOKEALERT:
                case PIR:
                case CMD:
                    payload = value.equalsIgnoreCase("on") ? "1" : "0";
                    break;
                case AWINSP:
                case RAINRATE:
                case RAIN:
                case TEMP:
                case WINSP:
                case WINCHL:
                case WINTMP:
                    payload = RFLinkUtils.getPayload(value, 10.0);
                    break;
                case BARO:
                case UV:
                case LUX:
                case WINGS:
                case WATT:
                case KWATT:
                    payload = RFLinkUtils.getPayload(value);
                    break;
                default:
                    payload = value;
                    break;
            }
        } else {
            throw new RawMessageException(
                    "Invalid option selected. either 'protocol' [or] 'key', 'value' is mandatory!");
        }
        RFLinkEngine.updateMessage(this);
    }

    public RFLinkRawMessage(McMessage mcMessage) throws RawMessageException {
        gatewayId = mcMessage.getGatewayId();
        nodeEui = mcMessage.getNodeEui();
        sensorId = mcMessage.getSensorId();
        messageType = mcMessage.getType();
        subType = mcMessage.getSubType();
        payload = mcMessage.getPayload();
        isTxMessage = mcMessage.isTxMessage();
        timestamp = mcMessage.getTimestamp();
        //Update protocol
        protocol = (String) mcMessage.getProperties().get(KEY_PROTOCOL);
        RFLinkEngine.updateMessage(this);
    }

    public void setPayload(Object payload) {
        payload = String.valueOf(payload);
    }

    public RawMessage getRawMessage() {
        if (protocol == null) {
            //TODO: cannot send this message
        }
        StringBuilder builder = new StringBuilder();
        builder
                .append("10;")
                .append(protocol).append(";")
                .append(nodeEui).append(";")
                .append(sensorId).append(";")
                .append(payload.equals("1") ? "ON" : "OFF").append(";");
        return RawMessage.builder()
                .gatewayId(gatewayId)
                .data(builder.toString())
                .build();
    }

    public McMessage getMcMessage() {
        return McMessage.builder()
                .acknowledge(false)
                .gatewayId(gatewayId)
                .nodeEui(nodeEui)
                .sensorId(sensorId)
                .networkType(NETWORK_TYPE.PHANT_IO)
                .type(messageType)
                .subType(subType)
                .timestamp(timestamp)
                .isTxMessage(isTxMessage())
                .payload(getPayload()).build();
    }
}