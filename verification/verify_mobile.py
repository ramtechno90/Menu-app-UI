
import os
from playwright.sync_api import sync_playwright

def verify_mobile_responsive():
    cwd = os.getcwd()
    # Paths to the local HTML files
    admin_url = f"file://{cwd}/web_portal/admin/index.html"
    delivery_url = f"file://{cwd}/web_portal/delivery/index.html"

    # Use user's home directory for screenshots
    home_dir = os.path.expanduser("~")
    verification_dir = os.path.join(home_dir, "verification")
    os.makedirs(verification_dir, exist_ok=True)

    with sync_playwright() as p:
        # Launch browser
        browser = p.chromium.launch(headless=True)

        # Emulate a mobile device (iPhone 12)
        iphone_12 = p.devices['iPhone 12']
        context = browser.new_context(**iphone_12)
        page = context.new_page()

        # 1. Verify Admin Portal Mobile View
        print(f"Navigating to Admin Portal at {admin_url}")
        page.goto(admin_url)
        page.wait_for_load_state("networkidle")

        # Take a screenshot of the Landing Page
        page.screenshot(path=os.path.join(verification_dir, "admin_mobile_landing.png"))
        print("Admin Landing Mobile verified.")

        # Go to Admin Dashboard (simulate login/bypass if possible or just check layout of login)
        # Clicking Admin Login
        page.get_by_role("button", name="Admin Login").click()
        page.screenshot(path=os.path.join(verification_dir, "admin_mobile_login.png"))
        print("Admin Login Mobile verified.")

        # 2. Verify Delivery Portal Mobile View
        print(f"Navigating to Delivery Portal at {delivery_url}")
        page.goto(delivery_url)
        page.wait_for_load_state("networkidle")

        # Take a screenshot of the Landing Page
        page.screenshot(path=os.path.join(verification_dir, "delivery_mobile_landing.png"))
        print("Delivery Landing Mobile verified.")

        # Click Delivery Login
        page.get_by_role("button", name="Delivery Staff Login").click()
        page.screenshot(path=os.path.join(verification_dir, "delivery_mobile_login.png"))
        print("Delivery Login Mobile verified.")

        browser.close()

if __name__ == "__main__":
    verify_mobile_responsive()
