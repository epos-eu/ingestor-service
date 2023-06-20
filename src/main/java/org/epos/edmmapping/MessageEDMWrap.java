package org.epos.edmmapping;

import java.util.Objects;

public class MessageEDMWrap {

    private String objectName;
    private String object;

    public MessageEDMWrap(String objectName, String object) {
        this.objectName = objectName;
        this.object = object;
    }

    public MessageEDMWrap() {
    }

    @Override
    public String toString() {
        return "MessageEDMWrap{" +
                "objectName='" + objectName + '\'' +
                ", object='" + object + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEDMWrap wrap = (MessageEDMWrap) o;
        return Objects.equals(getObjectName(), wrap.getObjectName()) && Objects.equals(getObject(), wrap.getObject());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getObjectName(), getObject());
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
}
