
import os
from playwright.sync_api import sync_playwright, expect

def verify_web_portal():
    cwd = os.getcwd()
    app_path = f"file://{cwd}/web_portal/index.html"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. Landing Page
        print(f"Navigating to {app_path}")
        page.goto(app_path)
        expect(page).to_have_title("Pizza Paradize Web Portal")
        expect(page.get_by_text("Pizza Paradize Portal")).to_be_visible()
        page.screenshot(path="/home/jules/verification/1_landing.png")
        print("Landing verified.")

        # 2. Click Admin Login
        # Use button locator to be specific
        page.get_by_role("button", name="Admin Login").click()
        expect(page.get_by_role("heading", name="Admin Login")).to_be_visible()
        page.screenshot(path="/home/jules/verification/2_admin_login.png")
        print("Admin Login verified.")

        # 3. Back to Landing
        page.get_by_role("button", name="Back").first.click()
        expect(page.get_by_text("Pizza Paradize Portal")).to_be_visible()

        # 4. Click Delivery Login
        page.get_by_role("button", name="Delivery Staff Login").click()
        expect(page.get_by_role("heading", name="Delivery Staff Login")).to_be_visible()
        page.screenshot(path="/home/jules/verification/3_delivery_login.png")
        print("Delivery Login verified.")

        browser.close()

if __name__ == "__main__":
    verify_web_portal()
