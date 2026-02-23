public class GymAddOnPricing implements AddOnPricing {
    @Override
    public boolean supports(AddOn addOn) {
        return addOn == AddOn.GYM;
    }

    @Override
    public double price() {
        return 300.0;
    }
}
