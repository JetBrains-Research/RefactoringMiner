package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.CodeRange;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}
