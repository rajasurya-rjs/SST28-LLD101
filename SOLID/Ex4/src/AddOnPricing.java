public interface AddOnPricing {
    boolean supports(AddOn addOn);

    double price();
}
