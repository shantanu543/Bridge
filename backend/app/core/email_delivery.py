import asyncio
import smtplib
from email.message import EmailMessage
from email.utils import parseaddr

from app.core.config import get_settings


def _send_email_sync(to_address: str, subject: str, body: str) -> None:
    settings = get_settings()
    message = EmailMessage()
    message["From"] = settings.email_from
    message["To"] = to_address
    message["Subject"] = subject
    message.set_content(body)
    with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10) as client:
        client.ehlo()
        if settings.smtp_starttls:
            client.starttls()
            client.ehlo()
        if settings.smtp_username and settings.smtp_password:
            client.login(settings.smtp_username, settings.smtp_password.get_secret_value())
        client.send_message(message)


async def send_security_code(to_address: str, code: str, purpose: str) -> None:
    if purpose == "verify_email":
        subject = "Verify your Bridge email"
        body = f"Your Bridge verification code is {code}. It expires in 10 minutes."
    else:
        subject = "Reset your Bridge password"
        body = f"Your Bridge password reset code is {code}. It expires in 10 minutes."
    await asyncio.to_thread(_send_email_sync, parseaddr(to_address)[1], subject, body)
