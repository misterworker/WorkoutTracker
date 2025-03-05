from firebase_functions import https_fn
from google.auth import default
from googleapiclient.discovery import build
from firebase_admin import initialize_app
from datetime import datetime, timezone

# Initialize the Firebase Admin SDK
initialize_app()


@https_fn.on_call(
    enforce_app_check=False  # Reject requests with missing or invalid App Check tokens.
)
def verify_purchase(req: https_fn.CallableRequest) -> dict:
    """Callable Cloud Function to verify Android subscription purchases."""
    try:
        # Use Application Default Credentials (ADC)
        credentials, _ = default(scopes=['https://www.googleapis.com/auth/androidpublisher'])
        androidpublisher = build('androidpublisher', 'v3', credentials=credentials)
    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message="Failed to initialize credentials or service",
            details=str(e)
        )

    purchase_token = req.data.get('token')
    package_name = "com.workoutwrecker.workouttracker"

    if not purchase_token:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Token is required"
        )

    try:
        subscription = androidpublisher.purchases().subscriptionsv2().get(
            packageName=package_name,
            token=purchase_token
        ).execute()

        # Extract details from the lineItems array
        line_items = subscription.get('lineItems', [])
        print(f"Line Items: {line_items}")

        if line_items:
            base_plan_id = line_items[0].get('offerDetails', {}).get('basePlanId')
            expiry_time = line_items[0].get('expiryTime')
            if not (base_plan_id or expiry_time):
                raise Exception("Missing base plan ID or expiry time")
        else:
            raise Exception("No line items found")

        print(f"Base Plan ID: {base_plan_id}")

        # Extract relevant data from the API response
        payment_state = subscription.get('subscriptionState')
        start_time = subscription.get('startTime')

        # Standardize the purchase time (use UTC as standard)
        # formatted_purchase_time = datetime.fromtimestamp(int(start_time)/1000, tz=timezone.utc).isoformat()
        # formatted_expiry_time = datetime.fromtimestamp(int(expiry_time)/1000, tz=timezone.utc).isoformat()

        # Return additional data to the client
        return {
            "basePlanId": base_plan_id,
            "purchaseToken": purchase_token,
            "purchaseTime": start_time,
            "paymentState": payment_state,
            "expiryTime": expiry_time
        }

    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNKNOWN,
            message="Error verifying subscription",
            details=str(e)
        )
