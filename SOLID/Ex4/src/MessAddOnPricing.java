public class MessAddOnPricing implements AddOnPricing {
    @Override
    public boolean supports(AddOn addOn) {
        return addOn == AddOn.MESS;
    }

    @Override
    public double price() {
        return 1000.0;
    }
}
