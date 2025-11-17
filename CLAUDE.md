# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SIFISO AI Phone** is an autonomous AI companion device featuring on-device intelligence, proactive wellness management, and privacy-first architecture. The project implements a complete software stack for a custom AI-powered mobile device with specialized NPU hardware, biosensors, and autonomous agent systems.

## Development Commands

### Running Core Components

```bash
# Run the complete implementation demonstration
python sifiso_complete_implementation.py

# Run individual module demonstrations
python sifiso_core_files.py
python sifiso_routines_ui.py
```

### Testing

Currently no formal test suite exists. When implementing tests, they should cover:
- AI agent decision-making logic
- Sensor data processing and fusion
- Privacy and permission management
- Routine execution and orchestration

## Architecture Overview

### Core System Layers

```
User Applications
       ↓
AI Agent Layer (Wellness, Travel, Career, Finance agents)
       ↓
Inference Engine (ONNX Runtime + NPU acceleration)
       ↓
Sensor Fusion & Processing (Biosensors + Environmental)
       ↓
Custom Linux Kernel + NPU Drivers
```

### Key Architectural Components

#### 1. **AI Engine** (`/ai_engine/`)
- **Inference Engine**: ONNX Runtime-based with custom NPU acceleration for on-device ML
- **Model Support**: LLM (7B parameters, 4-bit quantized), vision, voice, and predictive models
- **Agent Orchestrator**: Manages multiple AI agents for complex multi-step autonomous tasks
- **Priority System**: Tasks are queued by priority (CRITICAL > HIGH > NORMAL > LOW)

#### 2. **Autonomous Agents** (`/ai_engine/agents/`)
Each agent is specialized and operates independently:
- **WellnessAgent**: Continuous biosensor monitoring, stress detection (HRV analysis), proactive intervention
- **CareerAgent**: Industry trend analysis, skill gap identification, opportunity matching
- **TravelAgent**: Real-time disruption detection, autonomous rerouting, contact notification
- Other agents follow similar patterns of continuous monitoring → analysis → autonomous action

#### 3. **Sensor Systems** (`/sensors/`)
- **Biosensors**: Heart rate, HRV (frequency-domain FFT analysis), skin conductance, hydration
- **Environmental**: Air quality, ambient light, temperature, noise levels
- **Motion**: Accelerometer, gyroscope, gesture recognition, activity classification
- **Sensor Fusion**: Real-time multi-signal integration for context awareness

#### 4. **Autonomous Routines** (`/routines/`)
Routines execute without explicit user commands:
- **WakeUpRoutine**: Detects waking through motion/HR changes → coffee automation → news curation → smart home control
- **TravelDisruptionHandler**: Monitors trips → detects delays → reroutes → notifies contacts → updates calendar
- Routines are context-aware and adapt based on calendar, weather, and user patterns

#### 5. **Privacy Architecture** (`/privacy/`)
Three-tier privacy model:
- **Tier 1 (On-Device)**: Biometrics, sensor data, conversations - NEVER leaves device
- **Tier 2 (Encrypted Sync)**: Preferences, configs - E2E encrypted, optional
- **Tier 3 (Federated Learning)**: Model improvements - Differential privacy (ε=1.0), opt-in only
- **TransparencyLogger**: Records all data access with purpose, justification, and retention period

#### 6. **Generative UI** (`/ui/`)
- **DynamicThemeEngine**: Generates contextual themes based on time, activity, mood, environment
- **Adaptive Layouts**: UI morphs for different contexts (focus mode, wellness mode, etc.)
- **Color Psychology**: Theme adjustments based on detected stress, energy levels
- Flexible OLED display supports morphing (flat, curved, dual-screen, foldable)

### Critical Implementation Details

#### AI Power Management
- Predicts usage patterns to pre-allocate resources
- Dynamic CPU/GPU/NPU frequency scaling based on battery level and predicted demand
- Profile selection: maximum_performance, balanced, power_saver, ultra_saver
- Located in: `system/power_management/ai_power_optimizer.py`

#### Stress Analysis Algorithm
Multi-signal fusion approach:
1. Heart rate component (threshold: 90 bpm)
2. HRV component (RMSSD < 20 indicates stress)
3. LF/HF ratio from FFT analysis (ratio > 2.5 indicates sympathetic dominance)
4. Skin conductance (>10 indicates arousal)
5. Weighted combination: 0.3×HR + 0.35×HRV + 0.2×SC + 0.15×BR
- Located in: `sensors/biosensors/stress_analyzer.py`

#### Agent Communication Pattern
Agents communicate through the AgentOrchestrator:
```
User Context → AgentOrchestrator.orchestrate_routine()
           ↓
   [Parallel Task Execution]
           ↓
   Results Aggregation → User Notification
```
Key: Tasks with no dependencies run in parallel using `asyncio.gather()`

## File Organization Principles

### Project Structure (120+ directories, 200+ files)
- `/system/`: Kernel, drivers, power management, networking
- `/ai_engine/`: Models, inference, training, agents
- `/sensors/`: Biosensor processors, environmental monitors, motion tracking
- `/routines/`: Morning, travel, wellness, career automation scripts
- `/ui/`: Adaptive layouts, theme engine, voice/gesture interfaces
- `/privacy/`: Biometric auth, permissions, transparency logging, federated learning
- `/config/`: Device hardware specs, user preferences, AI behavior settings
- `/docs/`: User manual, developer guide, privacy policy

### Key Files

#### Implementation Files (Python)
- `sifiso_complete_implementation.py`: WellnessAgent and CareerAgent implementations
- `sifiso_core_files.py`: AIPowerOptimizer, InferenceEngine, AgentOrchestrator, StressAnalyzer
- `sifiso_routines_ui.py`: WakeUpRoutine, TravelDisruptionHandler, DynamicThemeEngine

#### Configuration Files
- `/privacy/federated_learning/fl_config.yaml`: Differential privacy parameters, secure aggregation settings
- `/config/device/hardware_config.yaml`: NPU specs (45 TOPS), biosensor types, display capabilities
- `/config/user_preferences/ai_behavior.yaml`: Autonomy level, proactivity settings, safety guardrails

#### Documentation
- `sifiso_project_summary.md`: Complete project overview, deliverables, roadmap
- `sifiso_structure.txt`: Full 200+ file directory tree
- `sifiso_privacy_config.txt`: Privacy architecture details

## Development Guidelines

### Agent Development Pattern
When creating new agents:
1. Inherit from base Agent class
2. Implement `async def monitor_*()` for continuous monitoring
3. Implement `async def analyze_*()` for decision-making
4. Implement `async def intervene()` for autonomous actions
5. Always log decisions via TransparencyLogger
6. Request minimal permissions (privacy by design)

### Sensor Data Processing
- All sensor data flows through sensor fusion pipeline
- Sampling rates vary: biosensors (100Hz), environmental (1Hz), motion (200Hz)
- Use moving averages and Kalman filters for noise reduction
- Time-domain + frequency-domain analysis for HRV metrics

### Privacy Requirements
- Never access Tier 1 data without explicit purpose logging
- All data access must go through PermissionManager
- Include justification strings in all sensor/data requests
- Federated learning requires battery >30%, WiFi, and device idle

### Model Optimization
- All models must be quantized (4-bit or 8-bit) for NPU efficiency
- Use ONNX format for inference engine compatibility
- Model caching is automatic via InferenceEngine
- Target inference latency: LLM <100ms/token, vision <33ms (30 FPS)

### UI Generation
- Themes regenerate on context change (time, activity, mood, environment)
- Color adjustments use HSV color space
- Transition animations adapt to detected mood (stressed → subtle, energetic → dynamic)
- Display morphing transitions take 500ms

## Hardware Specifications

### Custom NPU
- 45 TOPS (INT8), 15 TFLOPS (FP16), 90 TOPS (INT4)
- 6GB dedicated memory, 120 GB/s bandwidth
- TDP: 3.5W peak, 0.1W idle
- Specialized units: transformer accelerator, vision processor, audio DSP, sensor fusion

### Biosensors
- Heart rate: PPG sensor at 100Hz, medical-grade accuracy
- Stress: HRV + skin conductance + breathing rate
- Hydration: Bioimpedance sensor
- Blood oxygen: Pulse oximetry (98% accuracy)

### Memory & Storage
- 16GB LPDDR5X RAM @ 8533 MHz
- 512GB UFS 4.0 storage (AES-256 encrypted)

## Performance Targets

- LLM inference: 15-20 tokens/second (7B model, 4-bit quantization)
- Vision processing: 30 FPS object detection
- Voice recognition: <100ms latency
- Wake detection: <500ms from motion to routine initiation
- Theme generation: <100ms
- Battery life: 2 days with AI optimization, 1.5 days standard

## Privacy & Security

### Data Handling
- Biometric data stored in secure enclave only
- All cloud sync is end-to-end encrypted
- Differential privacy: ε=1.0, δ=1e-5
- Federated learning requires minimum 10 participants
- Transparency logs accessible via: Settings > Privacy > Data Usage

### Permission Model
- Granular, per-agent permissions
- Temporal permissions (e.g., "for next 24 hours")
- Purpose-required: every permission request includes explicit justification
- User can revoke at any time

## Known Limitations

1. **No Actual Hardware**: This is a software architecture/concept; custom NPU and biosensors don't physically exist
2. **Simplified ML Models**: Production models are placeholders; real models need training on actual user data
3. **No Real External Integrations**: Weather, traffic, news APIs are simulated
4. **Single-Device Focus**: Cross-device synchronization not fully implemented

## Project Status

- **Phase**: Architecture complete + core implementation
- **Lines of Code**: ~3,500 (core modules)
- **Readiness**: Prototype-ready (software stack complete, hardware conceptual)
- **Next Milestone**: Android app prototype with simulated sensors

## Business Context

- **Target Market**: Tech professionals 25-45, health-conscious consumers, enterprise wellness
- **Revenue Model**: Hardware sales ($899) + subscription ($4.99-9.99/month) + developer platform (20% revenue share)
- **Competitive Advantage**: True on-device AI, proactive autonomy, privacy-first architecture
- **Funding Status**: Pre-seed (architecture phase), seeking $1.2M seed round

## Contact & Vision

> "SIFISO represents more than technology—it's a philosophy of human-centered AI. Technology should amplify human potential while respecting privacy, autonomy, and wellbeing."

The project demonstrates feasibility of autonomous AI companions with complete on-device processing, ethical AI practices, and genuine user benefit without compromising privacy.
