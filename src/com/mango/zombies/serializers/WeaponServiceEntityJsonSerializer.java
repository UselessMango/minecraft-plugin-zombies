package com.mango.zombies.serializers;

import com.google.gson.*;
import com.mango.zombies.entities.WeaponServiceCharacteristicEntity;
import com.mango.zombies.entities.WeaponServiceEntity;

import java.lang.reflect.Type;
import java.util.UUID;

public class WeaponServiceEntityJsonSerializer implements JsonSerializer<WeaponServiceEntity>, JsonDeserializer<WeaponServiceEntity> {

    @Override
    public WeaponServiceEntity deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {

        JsonObject jsonObject = jsonElement.getAsJsonObject();

        WeaponServiceEntity service = new WeaponServiceEntity();

        service.setDamage(jsonObject.get(WeaponServiceEntity.DAMAGE_JSON_TAG).getAsInt());
        service.setTypeUUID(UUID.fromString(jsonObject.get(WeaponServiceEntity.TYPE_UUID_JSON_TAG).getAsString()));

        for (JsonElement jsonCharacteristic : jsonObject.get(WeaponServiceEntity.CHARACTERISTICS_JSON_TAG).getAsJsonArray())
            service.getCharacteristics().add(WeaponServiceCharacteristicEntity.SERIALIZER.deserialize(jsonCharacteristic, WeaponServiceCharacteristicEntity.class, jsonDeserializationContext));

        return service;
    }

    @Override
    public JsonElement serialize(WeaponServiceEntity weaponServiceEntity, Type type, JsonSerializationContext jsonSerializationContext) {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add(WeaponServiceEntity.DAMAGE_JSON_TAG, new JsonPrimitive(weaponServiceEntity.getDamage()));
        jsonObject.add(WeaponServiceEntity.TYPE_UUID_JSON_TAG, new JsonPrimitive(weaponServiceEntity.getTypeUUID().toString()));

        JsonArray characteristicsArray = new JsonArray();

        for (WeaponServiceCharacteristicEntity characteristic : weaponServiceEntity.getCharacteristics())
            characteristicsArray.add(WeaponServiceCharacteristicEntity.SERIALIZER.serialize(characteristic, WeaponServiceCharacteristicEntity.class, jsonSerializationContext));

        jsonObject.add(WeaponServiceEntity.CHARACTERISTICS_JSON_TAG, characteristicsArray);

        return jsonObject;
    }
}
