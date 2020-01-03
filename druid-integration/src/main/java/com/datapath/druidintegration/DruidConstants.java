package com.datapath.druidintegration;

import lombok.NoArgsConstructor;

import java.time.Year;

@NoArgsConstructor
public final class DruidConstants {

    public static final String DEFAULT_INTERVAL = "2015/" + Year.now().plusYears(1).getValue();

}
