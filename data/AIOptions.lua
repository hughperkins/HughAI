--
--  Custom Options Definition Table format
--
--  NOTES:
--  - using an enumerated table lets you specify the options order
--
--  These keywords must be lowercase for LuaParser to read them.
--
--  key:      the string used in the script.txt
--  name:     the displayed name
--  desc:     the description (could be used as a tooltip)
--  type:     the option type
--  def:      the default value;
--  min:      minimum value for number options
--  max:      maximum value for number options
--  step:     quantization step, aligned to the def value
--  maxlen:   the maximum string length for string options
--  items:    array of item strings for list options
--  scope:    'all', 'player', 'team', 'allyteam'      <<< not supported yet >>>
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

local options = {
	{
		key="maphack",
		name="Activatd Maphack",
		desc="does the AI use maphack to find your units?",
		type='bool',
		def=true,
	},
	{
		key="guiactivated",
		name="Activate GUI",
		desc="Activates GUI. Mostly useful for developers.  Make sure to play in windowed mode if this is activated!",
		type='bool',
		def=true,
	},
	{
		key="debug",
		name="Activated debug",
		desc="Shows lots of debug info, lines, ghost units and so on, so you can see what the AI is thinking.  Turn off for normal usage."
		type='bool',
		def=false,
	},
}

return options
