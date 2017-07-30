package edu.sdsu.its.API.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Tom Paulus
 * Created on 7/29/17.
 */
@Data
@Entity
@Table(name = "preferences")
@AllArgsConstructor
@NoArgsConstructor
public class Preference {
    @Id
    private String setting;

    private String value;
}
