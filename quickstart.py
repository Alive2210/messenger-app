#!/usr/bin/env python3
"""
Secure Messenger - Cross-Platform Quick Start
Works on: Windows, macOS, Linux

Usage:
    python quickstart.py
    python quickstart.py install
    python quickstart.py start
    python quickstart.py stop
    python quickstart.py logs
"""

import os
import sys
import platform
import subprocess
import shutil
from pathlib import Path

# Colors for terminal output
class Colors:
    GREEN = '\033[92m' if platform.system() != 'Windows' else ''
    RED = '\033[91m' if platform.system() != 'Windows' else ''
    YELLOW = '\033[93m' if platform.system() != 'Windows' else ''
    BLUE = '\033[94m' if platform.system() != 'Windows' else ''
    NC = '\033[0m' if platform.system() != 'Windows' else ''

def print_header():
    print("╔════════════════════════════════════════════════════════════╗")
    print("║         🚀 SECURE MESSENGER - QUICK START                  ║")
    print("╚════════════════════════════════════════════════════════════╝")
    print()

def print_success(msg):
    print(f"{Colors.GREEN}✅ {msg}{Colors.NC}")

def print_error(msg):
    print(f"{Colors.RED}❌ {msg}{Colors.NC}")

def print_warning(msg):
    print(f"{Colors.YELLOW}⚠️  {msg}{Colors.NC}")

def print_info(msg):
    print(f"{Colors.BLUE}ℹ️  {msg}{Colors.NC}")

def run_command(cmd, shell=True, check=True):
    """Run a shell command"""
    try:
        result = subprocess.run(cmd, shell=shell, check=check, capture_output=True, text=True)
        return result.returncode == 0
    except subprocess.CalledProcessError as e:
        return False

def check_docker():
    """Check if Docker is installed and running"""
    print_info("Checking Docker...")
    
    # Check if docker command exists
    docker_cmd = "docker"
    if platform.system() == "Windows":
        docker_cmd = "docker.exe"
    
    if not shutil.which("docker"):
        print_error("Docker is not installed!")
        print_info("Please install Docker:")
        if platform.system() == "Windows":
            print("  https://www.docker.com/products/docker-desktop")
        elif platform.system() == "Darwin":
            print("  https://docs.docker.com/desktop/install/mac-install/")
        else:
            print("  https://docs.docker.com/engine/install/")
        return False
    
    # Check if Docker is running
    try:
        subprocess.run(["docker", "info"], check=True, capture_output=True)
        print_success("Docker is running")
        return True
    except:
        print_error("Docker is not running!")
        print_info("Please start Docker Desktop")
        return False

def check_java():
    """Check if Java is installed"""
    print_info("Checking Java...")
    
    if shutil.which("java"):
        try:
            result = subprocess.run(["java", "-version"], capture_output=True, text=True)
            version_line = result.stderr.split('\n')[0]
            print_success(f"Java found: {version_line}")
            return True
        except:
            pass
    
    print_warning("Java not found. Will use Docker for building.")
    return False

def get_docker_compose_cmd():
    """Get the correct docker compose command"""
    # Try 'docker compose' first (newer versions)
    try:
        subprocess.run(["docker", "compose", "version"], check=True, capture_output=True)
        return "docker compose"
    except:
        pass
    
    # Try 'docker-compose' (older versions)
    if shutil.which("docker-compose"):
        return "docker-compose"
    
    return None

def install():
    """Install and setup the application"""
    print_header()
    
    # Check prerequisites
    if not check_docker():
        return False
    
    has_java = check_java()
    
    # Check docker compose
    compose_cmd = get_docker_compose_cmd()
    if not compose_cmd:
        print_error("Docker Compose not found!")
        return False
    print_success(f"Docker Compose found: {compose_cmd}")
    
    # Create directories
    print_info("Creating directories...")
    dirs = ["logs", "data/postgres", "data/minio", "data/rabbitmq"]
    for d in dirs:
        Path(d).mkdir(parents=True, exist_ok=True)
    print_success("Directories created")
    
    # Generate .env file
    env_file = Path(".env")
    if not env_file.exists():
        print_info("Generating .env file with secure passwords...")
        import secrets
        import string
        
        def generate_password(length):
            alphabet = string.ascii_letters + string.digits
            return ''.join(secrets.choice(alphabet) for _ in range(length))
        
        env_content = f"""# Database Configuration
DB_USERNAME=messenger_user
DB_PASSWORD={generate_password(32)}
DB_NAME=messenger_db

# JWT Configuration
JWT_SECRET={generate_password(64)}

# MinIO Configuration
MINIO_ACCESS_KEY={generate_password(24)}
MINIO_SECRET_KEY={generate_password(48)}

# RabbitMQ Configuration
RABBITMQ_USER=messenger
RABBITMQ_PASS={generate_password(24)}

# Application Configuration
SERVER_URL=http://localhost:8080
WEBSOCKET_URL=/ws

# Generated on {platform.system()}
"""
        env_file.write_text(env_content)
        print_success(".env file created")
    else:
        print_success(".env file already exists")
    
    # Build application
    print_info("Building application...")
    if has_java and shutil.which("mvn"):
        if run_command("mvn clean package -DskipTests"):
            print_success("Build successful")
        else:
            print_error("Build failed!")
            return False
    else:
        print_info("Building with Docker...")
        if run_command("docker build -t messenger-app ."):
            print_success("Docker build successful")
        else:
            print_error("Docker build failed!")
            return False
    
    print()
    print_success("Installation complete!")
    return True

def start():
    """Start all services (with auto-install if needed)"""
    print_header()
    
    # Check if .env exists - if not, run install first
    if not Path(".env").exists():
        print_info(".env file not found! Running installation first...")
        print()
        if not install():
            return False
        print()
    
    if not check_docker():
        return False
    
    compose_cmd = get_docker_compose_cmd()
    if not compose_cmd:
        print_error("Docker Compose not found!")
        return False
    
    print_info("Starting services...")
    if run_command(f"{compose_cmd} up -d"):
        print_success("Services started")
        
        print()
        print("╔════════════════════════════════════════════════════════════╗")
        print("║         🎉 SERVER STARTED SUCCESSFULLY!                    ║")
        print("╚════════════════════════════════════════════════════════════╝")
        print()
        print("🌐 Access URLs:")
        print("   • Application:     http://localhost:8080")
        print("   • Health Check:    http://localhost:8080/actuator/health")
        print("   • RabbitMQ:        http://localhost:15672 (guest/guest)")
        print("   • MinIO Console:   http://localhost:9001")
        print()
        print("💡 Useful commands:")
        print("   python quickstart.py logs     - View logs")
        print("   python quickstart.py stop     - Stop services")
        print("   python quickstart.py restart  - Restart services")
        print()
        return True
    else:
        print_error("Failed to start services")
        return False

def stop():
    """Stop all services"""
    print_info("Stopping services...")
    
    compose_cmd = get_docker_compose_cmd()
    if compose_cmd:
        if run_command(f"{compose_cmd} down"):
            print_success("Services stopped")
            return True
    
    print_error("Failed to stop services")
    return False

def restart():
    """Restart all services"""
    print_info("Restarting services...")
    
    compose_cmd = get_docker_compose_cmd()
    if compose_cmd:
        if run_command(f"{compose_cmd} restart"):
            print_success("Services restarted")
            return True
    
    print_error("Failed to restart services")
    return False

def logs():
    """View logs"""
    compose_cmd = get_docker_compose_cmd()
    if compose_cmd:
        print_info("Showing logs (Press Ctrl+C to exit)...")
        try:
            subprocess.run(f"{compose_cmd} logs -f", shell=True)
        except KeyboardInterrupt:
            print()
            print_info("Stopped viewing logs")
        return True
    return False

def status():
    """Check status"""
    compose_cmd = get_docker_compose_cmd()
    if compose_cmd:
        print_info("Service Status:")
        run_command(f"{compose_cmd} ps", check=False)
        return True
    return False

def test():
    """Run tests"""
    print_info("Running tests...")
    
    if shutil.which("mvn"):
        if run_command("mvn test", check=False):
            print_success("Tests completed")
            return True
        else:
            print_warning("Some tests failed")
            return False
    else:
        print_error("Maven not found. Cannot run tests.")
        return False

def main():
    """Main entry point"""
    if len(sys.argv) < 2:
        # Default: start (with auto-install if needed)
        start()
    else:
        command = sys.argv[1].lower()
        
        if command == "install":
            install()
        elif command == "start":
            start()
        elif command == "stop":
            stop()
        elif command == "restart":
            restart()
        elif command == "logs":
            logs()
        elif command == "status":
            status()
        elif command == "test":
            test()
        elif command in ["--help", "-h", "help"]:
            print_header()
            print("Usage: python quickstart.py [command]")
            print()
            print("Commands:")
            print("  (no command)  - Install and start services")
            print("  install       - Install and build application")
            print("  start         - Start all services")
            print("  stop          - Stop all services")
            print("  restart       - Restart all services")
            print("  logs          - View logs in real-time")
            print("  status        - Check service status")
            print("  test          - Run tests")
            print()
            print("Platform:", platform.system())
        else:
            print_error(f"Unknown command: {command}")
            print_info("Run 'python quickstart.py --help' for usage")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print()
        print_info("Operation cancelled by user")
        sys.exit(0)
    except Exception as e:
        print_error(f"An error occurred: {e}")
        sys.exit(1)
