package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.UUID;

/**
 * Created by stephenblack on 11/6/14.
 */

@Table(name = "TransmitterData", id = BaseColumns._ID)
public class TransmitterData extends Model {
    private final static String TAG = TransmitterData.class.getSimpleName();

    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Column(name = "raw_data")
    public double raw_data;

    @Column(name = "filtered_data")
    public double filtered_data;

    @Column(name = "sensor_battery_level")
    public int sensor_battery_level;

    @Column(name = "uuid", index = true)
    public String uuid;

    public static synchronized TransmitterData create(byte[] buffer, int len, Long timestamp) {
        if (len < 6) {
            return null;
        }
        TransmitterData transmitterData = new TransmitterData();
        if (buffer[0] == 0x11 && buffer[1] == 0x00) {
            //this is a dexbridge packet.  Process accordingly.
            Log.i(TAG, "create Processing a Dexbridge packet");
            ByteBuffer txData = ByteBuffer.allocate(len);
            txData.order(ByteOrder.LITTLE_ENDIAN);
            txData.put(buffer, 0, len);
            transmitterData.raw_data = txData.getInt(2);
            transmitterData.filtered_data = txData.getInt(6);
            //  bitwise and with 0xff (1111....1) to avoid that the byte is treated as signed.
            transmitterData.sensor_battery_level = txData.get(10) & 0xff;
            Log.i(TAG, "Created transmitterData record with Raw value of " + transmitterData.raw_data + " and Filtered value of " + transmitterData.filtered_data + " at " + transmitterData.timestamp);
        } else { //this is NOT a dexbridge packet.  Process accordingly.
            Log.i(TAG, "create Processing a BTWixel or IPWixel packet");
            StringBuilder data_string = new StringBuilder();
            for (int i = 0; i < len; ++i) {
                data_string.append((char) buffer[i]);
            }
            String[] data = data_string.toString().split("\\s+");

            if (data.length > 1) {
                transmitterData.sensor_battery_level = Integer.parseInt(data[1]);
            }
            transmitterData.raw_data = Integer.parseInt(data[0]);
            transmitterData.filtered_data = Integer.parseInt(data[0]);
        }
        //Stop allowing duplicate data, its bad!
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == transmitterData.raw_data && Math.abs(lastTransmitterData.timestamp - timestamp) < (120000)) {
            return null;
        }

        transmitterData.timestamp = timestamp;
        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static synchronized TransmitterData create(int raw_data, int sensor_battery_level, long timestamp) {
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (120000)) { //Stop allowing duplicate data, its bad!
            return null;
        }

        TransmitterData transmitterData = new TransmitterData();
        transmitterData.sensor_battery_level = sensor_battery_level;
        transmitterData.raw_data = raw_data;
        transmitterData.timestamp = timestamp;
        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static TransmitterData last() {
        return new Select()
                .from(TransmitterData.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

}
