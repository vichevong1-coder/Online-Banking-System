import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/theme/app_theme.dart';

/// Six boxed digit cells with auto-advance, backspace-to-previous and paste
/// support. Reports the code on every change and fires [onCompleted] once six
/// digits are in, so the caller can submit without a button press.
class OtpCodeInput extends StatefulWidget {
  const OtpCodeInput({
    super.key,
    required this.onChanged,
    this.onCompleted,
    this.length = 6,
    this.hasError = false,
    this.autofocus = true,
  });

  final ValueChanged<String> onChanged;
  final ValueChanged<String>? onCompleted;
  final int length;
  final bool hasError;
  final bool autofocus;

  @override
  State<OtpCodeInput> createState() => OtpCodeInputState();
}

class OtpCodeInputState extends State<OtpCodeInput> {
  late final List<TextEditingController> _controllers;
  late final List<FocusNode> _focusNodes;
  // Separate nodes for the KeyboardListener wrappers: they only observe raw key
  // events (backspace on an empty box) and must never take focus themselves.
  late final List<FocusNode> _keyNodes;

  @override
  void initState() {
    super.initState();
    _controllers = List.generate(widget.length, (_) => TextEditingController());
    _focusNodes = List.generate(widget.length, (_) => FocusNode());
    _keyNodes = List.generate(widget.length, (_) => FocusNode(skipTraversal: true, canRequestFocus: false));
  }

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    for (final f in _focusNodes) {
      f.dispose();
    }
    for (final f in _keyNodes) {
      f.dispose();
    }
    super.dispose();
  }

  String get _code => _controllers.map((c) => c.text).join();

  /// Lets the parent wipe the boxes after a rejected code.
  void clear() {
    for (final c in _controllers) {
      c.clear();
    }
    widget.onChanged('');
    _focusNodes.first.requestFocus();
    setState(() {});
  }

  void _onChanged(int index, String value) {
    // A paste lands as several characters in one field — spread it across the
    // remaining boxes instead of dropping everything but the first digit.
    final digits = value.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length > 1) {
      for (var i = 0; i < widget.length - index; i++) {
        _controllers[index + i].text = i < digits.length ? digits[i] : '';
      }
      final next = (index + digits.length).clamp(0, widget.length - 1);
      _focusNodes[next].requestFocus();
    } else {
      _controllers[index].text = digits;
      if (digits.isNotEmpty && index < widget.length - 1) {
        _focusNodes[index + 1].requestFocus();
      }
    }

    setState(() {});
    final code = _code;
    widget.onChanged(code);
    if (code.length == widget.length) {
      FocusScope.of(context).unfocus();
      widget.onCompleted?.call(code);
    }
  }

  void _onKey(int index, KeyEvent event) {
    if (event is KeyDownEvent &&
        event.logicalKey == LogicalKeyboardKey.backspace &&
        _controllers[index].text.isEmpty &&
        index > 0) {
      _controllers[index - 1].clear();
      _focusNodes[index - 1].requestFocus();
      setState(() {});
      widget.onChanged(_code);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: List.generate(widget.length, (index) {
        final filled = _controllers[index].text.isNotEmpty;
        final focused = _focusNodes[index].hasFocus;

        return Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 5),
            child: KeyboardListener(
              focusNode: _keyNodes[index],
              onKeyEvent: (event) => _onKey(index, event),
              child: AspectRatio(
                aspectRatio: 0.82,
                child: Container(
                  decoration: BoxDecoration(
                    color: filled ? const Color(0x2600FFB2) : AppTheme.surfaceGlass,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(
                      color: widget.hasError
                          ? AppTheme.primaryRedLight
                          : (focused || filled ? AppTheme.emeraldLight : AppTheme.surfaceGlassBorder),
                      width: focused ? 1.8 : 1.2,
                    ),
                  ),
                  child: Center(
                    child: TextField(
                      controller: _controllers[index],
                      focusNode: _focusNodes[index],
                      autofocus: widget.autofocus && index == 0,
                      keyboardType: TextInputType.number,
                      textAlign: TextAlign.center,
                      showCursor: false,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                      decoration: const InputDecoration(
                        counterText: '',
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        contentPadding: EdgeInsets.zero,
                      ),
                      onTap: () => setState(() {}),
                      onChanged: (value) => _onChanged(index, value),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      }),
    );
  }
}
