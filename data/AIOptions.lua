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
		type='list',
		def='yes',
		items='no','yes',
	},
	{
		key="guiactivated",
		name="Activate GUI",
		desc="Activates GUI. Mostly useful for developers.  Make sure to play in windowed mode if this is activated!",
		type='list',
		def='yes',
		items='no','yes',
	},
	{
		key="debug",
		name="Activated debug",
		desc="Shows lots of debug info, lines, ghost units and so on, so you can see what the AI is thinking.  Turn off for normal usage."
		type='list',
		def='no',
		items='no','yes',
	},
	{
		key="defaultworkflowname",
		name="Default workflow name",
		desc="Selects the build workflow to use.  By default there is only the default workflow, but you can create others, and switch between them here.",
		type='list',
		def='default',
		items='default',
	},
}

return options
