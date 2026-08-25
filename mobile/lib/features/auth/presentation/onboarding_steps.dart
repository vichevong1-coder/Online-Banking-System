import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';

/// Registration is three screens — phone (1), KYC details (2), OTP verify (3).
/// Every one of them shows this so the customer knows how much is left.
class OnboardingSteps extends StatelessWidget {
  const OnboardingSteps({super.key, required this.currentStep, this.totalSteps = 3});

  final int currentStep;
  final int totalSteps;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var step = 1; step <= totalSteps; step++) ...[
          _StepDot(step: step, isDone: step < currentStep, isCurrent: step == currentStep),
          if (step < totalSteps)
            Expanded(
              child: Container(
                height: 2,
                margin: const EdgeInsets.symmetric(horizontal: 8),
                color: step < currentStep ? AppTheme.emeraldLight : Colors.white24,
              ),
            ),
        ],
        const SizedBox(width: 12),
        Text(
          'Step $currentStep of $totalSteps',
          style: const TextStyle(color: Colors.white70, fontSize: 12, fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
}

class _StepDot extends StatelessWidget {
  const _StepDot({required this.step, required this.isDone, required this.isCurrent});

  final int step;
  final bool isDone;
  final bool isCurrent;

  @override
  Widget build(BuildContext context) {
    final isActive = isDone || isCurrent;
    return Container(
      width: 26,
      height: 26,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isDone ? AppTheme.emeraldLight : (isCurrent ? const Color(0x3300FFB2) : Colors.transparent),
        border: Border.all(color: isActive ? AppTheme.emeraldLight : Colors.white24, width: 1.4),
      ),
      child: Center(
        child: isDone
            ? const Icon(Icons.check, size: 14, color: Color(0xFF06221E))
            : Text(
                '$step',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: isCurrent ? AppTheme.emeraldLight : Colors.white38,
                ),
              ),
      ),
    );
  }
}
