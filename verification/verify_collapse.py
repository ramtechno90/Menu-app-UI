
import os
from playwright.sync_api import sync_playwright

def verify_collapse():
    cwd = os.getcwd()
    admin_url = f"file://{cwd}/web_portal/admin/index.html"
    delivery_url = f"file://{cwd}/web_portal/delivery/index.html"

    # Use user's home directory for screenshots
    home_dir = os.path.expanduser("~")
    verification_dir = os.path.join(home_dir, "verification")
    os.makedirs(verification_dir, exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        iphone_12 = p.devices['iPhone 12']
        context = browser.new_context(**iphone_12)
        page = context.new_page()

        # 1. Verify Admin Portal
        print(f"Navigating to Admin Portal at {admin_url}")
        page.goto(admin_url)
        page.wait_for_load_state("networkidle")

        # Verify Nav is hidden
        # The nav element should not be visible in the viewport or have display: none
        # Note: Playwright's 'is_visible' checks CSS display/visibility.
        # nav selector is '#admin-nav'
        # But we need to be logged in to see the dashboard where the nav is.
        # The landing page doesn't have the header/nav.
        # So we verify the structure on Dashboard container by simulating it visible?
        # Or better, we verify the HTML/CSS rules applied if we can.
        # Let's bypass login view visibility for a second to verify layout.

        page.evaluate("document.getElementById('landing-container').style.display = 'none'")
        page.evaluate("document.getElementById('admin-dashboard-container').style.display = 'block'")

        # Now we are on dashboard view
        page.screenshot(path=os.path.join(verification_dir, "admin_mobile_dashboard_nav_collapsed.png"))

        # Check nav visibility
        # Note: 'nav' is hidden by css.
        nav_visible = page.is_visible("#admin-nav")
        print(f"Admin Nav Visible (Should be False): {nav_visible}")

        if nav_visible:
            print("ERROR: Admin Nav should be collapsed/hidden by default on mobile.")
            # Depending on strictness, we might not fail here but report it.
            # Actually, css `display: none` should result in False.

        # Click Toggle
        if page.is_visible("#nav-toggle"):
             page.click("#nav-toggle")
             page.screenshot(path=os.path.join(verification_dir, "admin_mobile_dashboard_nav_expanded.png"))
             nav_visible_after = page.is_visible("#admin-nav")
             print(f"Admin Nav Visible After Toggle (Should be True): {nav_visible_after}")
        else:
             print("ERROR: Nav toggle button not visible on mobile.")

        # 2. Verify Delivery Portal
        print(f"Navigating to Delivery Portal at {delivery_url}")
        page.goto(delivery_url)
        page.wait_for_load_state("networkidle")

        # Bypass login
        page.evaluate("document.getElementById('landing-container').style.display = 'none'")
        page.evaluate("document.getElementById('delivery-dashboard-container').style.display = 'block'")

        page.screenshot(path=os.path.join(verification_dir, "delivery_mobile_dashboard_nav_collapsed.png"))

        nav_visible = page.is_visible("#delivery-nav")
        print(f"Delivery Nav Visible (Should be False): {nav_visible}")

        # Click Toggle
        if page.is_visible("#nav-toggle"):
             page.click("#nav-toggle")
             page.screenshot(path=os.path.join(verification_dir, "delivery_mobile_dashboard_nav_expanded.png"))
             nav_visible_after = page.is_visible("#delivery-nav")
             print(f"Delivery Nav Visible After Toggle (Should be True): {nav_visible_after}")
        else:
             print("ERROR: Nav toggle button not visible on mobile.")

        browser.close()

if __name__ == "__main__":
    verify_collapse()
