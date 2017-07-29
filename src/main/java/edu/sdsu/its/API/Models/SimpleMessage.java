package edu.sdsu.its.API.Models;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Simple JSON Object that can be used to send a message when a JSON object is expected by the Client.
 *
 * @author Tom Paulus
 *         Created on 7/29/16.
 */
@SuppressWarnings("WeakerAccess")
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class SimpleMessage {
    @Expose
    private String status = null;
    @Expose
    private @NonNull String message;

    public String asJson() {
        return new Gson().toJson(this);
    }
}
