package org.waterwood.waterfunservicecore.services.location;

import java.util.Map;

public interface IpLocationService {
    Map<String, String> lookup(String ip);
}
