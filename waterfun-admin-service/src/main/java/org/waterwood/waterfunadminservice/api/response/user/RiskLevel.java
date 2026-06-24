package org.waterwood.waterfunadminservice.api.response.user;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,;

    public static RiskLevel calculate(long submitCnt, long rejectCnt, long reportCnt, long reportHitCnt) {
        // Protection
        if (submitCnt < 10) {
            return LOW;
        }

        double reportHitRate = reportCnt > 0 ? (double) reportHitCnt / reportCnt : 0.0;
        double rejectRate = (double) rejectCnt / submitCnt;

        boolean highReportHit = reportHitRate >= 0.5;   // report hit rate≥ 50%
        boolean highReject = rejectRate >= 0.3;          // content reject ≥ 30%

        if (highReportHit && highReject) {
            return HIGH;
        }
        if (highReportHit || highReject) {
            return MEDIUM;
        }
        return LOW;
    }
}
